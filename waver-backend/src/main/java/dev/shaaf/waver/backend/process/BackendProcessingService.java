package dev.shaaf.waver.backend.process;


import dev.shaaf.jgraphlet.TaskPipeline;
import dev.shaaf.waver.backend.FileUtil;
import dev.shaaf.waver.backend.WaverProcessEvent;
import dev.shaaf.waver.backend.config.MinioConfig;
import dev.shaaf.waver.backend.config.WaverConfig;
import dev.shaaf.waver.backend.minio.MinioUploaderTask;
import dev.shaaf.waver.llm.config.*;
import dev.shaaf.waver.llm.tutorial.task.*;
import dev.langchain4j.model.chat.ChatModel;
import io.minio.MinioClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

/**
 * Backend processing service responsible for handling tutorial generation requests.
 * <p>
 * This service orchestrates the complete tutorial generation pipeline, including:
 * <ul>
 *     <li>Processing incoming events from messaging system</li>
 *     <li>Configuring LLM providers (OpenAI, Gemini)</li>
 *     <li>Running the tutorial generation pipeline</li>
 *     <li>Uploading results to MinIO storage</li>
 * </ul>
 * <p>
 * The service is built on Quarkus framework using CDI for dependency injection
 * and MicroProfile Reactive Messaging for event processing.
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationScoped
public class BackendProcessingService {

    /** Logger instance for this service. */
    private static final Logger logger = Logger.getLogger(BackendProcessingService.class.getName());
    
    /** Injected configuration for Waver application settings. */
    @Inject
    WaverConfig waverConfig;
    
    /** Injected MinIO configuration settings. */
    @Inject
    MinioConfig minioConfig;
    
    /** Injected MinIO client for object storage operations. */
    @Inject
    MinioClient minioClient;

    /**
     * Handles incoming tutorial generation requests from the messaging system.
     * <p>
     * This method receives events asynchronously and triggers the tutorial generation
     * process for the specified source URL. The processing runs in a separate thread
     * to avoid blocking the messaging system.
     *
     * @param event The processing event containing the source URL to generate tutorials from
     * @return A CompletionStage that completes when the generation process finishes
     */
    @Incoming("requests")
    public CompletionStage<Void> initAndRunPipeline(WaverProcessEvent event) {
        logger.info("🚀 Event is invoked, starting generation: " + event.sourceUrl());
        return CompletableFuture.runAsync(() -> {
            try {
                generate(event.sourceUrl());
                logger.info("🚀 Generation has ended. Good bye! " + event.sourceUrl());
            } catch (Exception e) {
                logger.severe("❌ Error during tutorial generation: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Tutorial generation failed", e);
            }
        });
    }

    /**
     * Generates a tutorial from the specified input path.
     * <p>
     * This method creates the necessary configuration and delegates to the main
     * generation method. It validates the LLM provider configuration before proceeding.
     *
     * @param inputPath The path to the source code repository to generate tutorials from
     * @throws MissingConfigurationException if the LLM API key is missing
     */
    public void generate(String inputPath) {
        ProviderConfig providerConfig = getProviderConfig();
        if (providerConfig.getApiKey() == null) {
            throw new MissingConfigurationException("LLM API key is missing.");
        }

        generate(
                new AppConfig(inputPath,
                        getAbsolutePath(waverConfig.outputPath()),
                        waverConfig.llmProvider(),
                        providerConfig.getApiKey(),
                        waverConfig.verbose(),
                        FileUtil.getFolderNameFromInputPath(inputPath),
                        waverConfig.outputFormat(),
                        GenerationType.TUTORIAL)
        );
    }

    /**
     * Converts a relative or absolute path to an absolute path string.
     *
     * @param path The path to convert (can be relative or absolute)
     * @return The absolute path as a string
     */
    public String getAbsolutePath(String path) {
        return Paths.get(path).toAbsolutePath().toString();
    }


    /**
     * Main tutorial generation method that orchestrates the entire pipeline.
     * <p>
     * This method:
     * <ol>
     *     <li>Creates a ChatModel instance from the configured LLM provider</li>
     *     <li>Sets up the output directory structure</li>
     *     <li>Constructs and runs the task pipeline with all necessary steps</li>
     *     <li>Uploads the results to MinIO storage</li>
     * </ol>
     *
     * @param appConfig Complete application configuration for the generation process
     */
    public void generate(AppConfig appConfig) {
        logger.info("🚀 Starting Tutorial Generation for: " + appConfig.inputPath());
        ChatModel chatModel = ModelProviderFactory.buildChatModel(appConfig.llmProvider(), appConfig.apiKey());
        Path outputDir = Paths.get(appConfig.absoluteOutputPath() + "/" + appConfig.projectName());

        
        try (TaskPipeline tasksPipeLine = new TaskPipeline()) {
            tasksPipeLine.add("Code-crawler", new CodeCrawlerTask())
                    .then("Identify-abstraction", new IdentifyAbstractionsTask(chatModel, appConfig.projectName()))
                    .then("Identify-relationships", new IdentifyRelationshipsTask(chatModel, appConfig.projectName()))
                    .then("Chapter-organizer", new ChapterOrganizerTask(chatModel))
                    .then("Technical-writer", new TechnicalWriterTask(chatModel, outputDir))
                    .then("Meta-info", new MetaInfoTask(chatModel, outputDir, appConfig.projectName(), appConfig.inputPath()))
                    .then("Minio-upload", new MinioUploaderTask(minioClient, outputDir, minioConfig.bucketName()));
            tasksPipeLine.run(appConfig.inputPath()).join();
            logger.info("\n✅ Tutorial generation complete! Output located at: " + outputDir);
        }
    }

    /**
     * Retrieves the provider-specific configuration based on the selected LLM provider.
     * <p>
     * This method maps the configured LLM provider to its specific configuration,
     * including API keys and other provider-specific settings.
     *
     * @return The provider configuration containing API keys and settings
     * @throws MissingConfigurationException if the LLM provider is not supported or
     *                                     if required configuration is missing
     */
    public ProviderConfig getProviderConfig() {
        LLMProvider llmProvider = waverConfig.llmProvider();

        return switch (llmProvider) {
            case OpenAI -> new ProviderConfig.OpenAI(
                    waverConfig.openai().apiKey().orElseThrow(() ->
                            new MissingConfigurationException("Property 'waver.openai.api-key' was not set for OpenAI provider."))
            );
            case Gemini -> new ProviderConfig.Gemini(
                    waverConfig.gemini().apiKey().orElseThrow(() ->
                            new MissingConfigurationException("Property 'waver.gemini.api-key' was not set for Gemini provider."))
            );
            case null, default -> throw new MissingConfigurationException(
                    "Unsupported or missing LLM provider configured in 'waver.llmprovider'. Valid options are: " + Arrays.toString(LLMProvider.values())
            );
        };
    }

}
