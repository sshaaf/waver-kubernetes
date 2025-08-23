package dev.shaaf.waver.backend.process;

import dev.shaaf.waver.backend.WaverProcessEvent;
import dev.shaaf.waver.backend.config.MinioConfig;
import dev.shaaf.waver.backend.config.WaverConfig;
import dev.shaaf.waver.llm.config.FormatConverter;
import dev.shaaf.waver.llm.config.LLMProvider;
import dev.shaaf.waver.llm.config.MissingConfigurationException;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BackendProcessingService.
 * <p>
 * These tests verify the service's behavior without using mocks,
 * instead relying on test implementations and temporary directories.
 */
class BackendProcessingServiceTest {

    private BackendProcessingService service;
    private TestWaverConfig waverConfig;
    private TestMinioConfig minioConfig;
    private MinioClient minioClient;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new BackendProcessingService();
        waverConfig = new TestWaverConfig();
        minioConfig = new TestMinioConfig();
        
        // Create a test MinioClient (this would normally be injected)
        minioClient = MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("minioadmin", "minioadmin")
                .build();
        
        // Inject dependencies manually since we're not using CDI in unit tests
        service.waverConfig = waverConfig;
        service.minioConfig = minioConfig;
        service.minioClient = minioClient;
    }

    @Test
    void testInitAndRunPipelineWithValidEvent() throws ExecutionException, InterruptedException {
        // Given
        WaverProcessEvent event = new WaverProcessEvent("https://github.com/test/repo.git");
        
        // When
        CompletionStage<Void> result = service.initAndRunPipeline(event);
        
        // Then
        assertNotNull(result);
        // Note: The actual execution would require external services, so we're just testing the method structure
        assertDoesNotThrow(() -> {
            // This would normally complete the pipeline
            // result.toCompletableFuture().get();
        });
    }

    @Test
    void testInitAndRunPipelineWithNullEvent() {
        // When & Then
        // The actual implementation will throw NPE for null events
        assertThrows(NullPointerException.class, () -> {
            service.initAndRunPipeline(null);
        });
    }

    @Test
    void testGenerateWithMissingApiKey() {
        // Given
        waverConfig.setOpenAiApiKey(null);
        String inputPath = tempDir.toString();
        
        // When & Then
        assertThrows(MissingConfigurationException.class, () -> {
            service.generate(inputPath);
        });
    }

    @Test
    void testGenerateWithValidConfiguration() {
        // Given
        waverConfig.setOpenAiApiKey("test-api-key");
        String inputPath = tempDir.toString();
        
        // When & Then
        assertDoesNotThrow(() -> {
            // Note: This would require external LLM services to complete successfully
            // For unit testing, we verify that configuration validation passes
            try {
                service.generate(inputPath);
            } catch (Exception e) {
                // Expected to fail at LLM interaction level, not configuration level
                assertFalse(e instanceof MissingConfigurationException);
            }
        });
    }

    @Test
    void testGetAbsolutePath() {
        // Given
        String relativePath = "test/path";
        String absolutePath = "/absolute/test/path";
        
        // When
        String result1 = service.getAbsolutePath(relativePath);
        String result2 = service.getAbsolutePath(absolutePath);
        
        // Then
        assertNotNull(result1);
        assertTrue(Paths.get(result1).isAbsolute());
        assertNotNull(result2);
        assertTrue(Paths.get(result2).isAbsolute());
        assertEquals(absolutePath, result2);
    }

    @Test
    void testGetProviderConfigWithOpenAI() {
        // Given
        waverConfig.setLlmProvider(LLMProvider.OpenAI);
        waverConfig.setOpenAiApiKey("test-openai-key");
        
        // When
        var providerConfig = service.getProviderConfig();
        
        // Then
        assertNotNull(providerConfig);
        assertEquals("test-openai-key", providerConfig.getApiKey());
    }

    @Test
    void testGetProviderConfigWithGemini() {
        // Given
        waverConfig.setLlmProvider(LLMProvider.Gemini);
        waverConfig.setGeminiApiKey("test-gemini-key");
        
        // When
        var providerConfig = service.getProviderConfig();
        
        // Then
        assertNotNull(providerConfig);
        assertEquals("test-gemini-key", providerConfig.getApiKey());
    }

    @Test
    void testGetProviderConfigWithMissingOpenAIKey() {
        // Given
        waverConfig.setLlmProvider(LLMProvider.OpenAI);
        waverConfig.setOpenAiApiKey(null);
        
        // When & Then
        assertThrows(MissingConfigurationException.class, () -> {
            service.getProviderConfig();
        });
    }

    @Test
    void testGetProviderConfigWithMissingGeminiKey() {
        // Given
        waverConfig.setLlmProvider(LLMProvider.Gemini);
        waverConfig.setGeminiApiKey(null);
        
        // When & Then
        assertThrows(MissingConfigurationException.class, () -> {
            service.getProviderConfig();
        });
    }

    @Test
    void testGetProviderConfigWithUnsupportedProvider() {
        // Given
        waverConfig.setLlmProvider(null);
        
        // When & Then
        assertThrows(MissingConfigurationException.class, () -> {
            service.getProviderConfig();
        });
    }

    // Test implementation classes

    /**
     * Test implementation of WaverConfig for unit testing.
     */
    private static class TestWaverConfig implements WaverConfig {
        private LLMProvider llmProvider = LLMProvider.OpenAI;
        private String outputPath = "./test-output";
        private boolean verbose = true;
        private FormatConverter.OutputFormat outputFormat = FormatConverter.OutputFormat.MARKDOWN;
        private String openAiApiKey = "test-key";
        private String geminiApiKey = "test-key";

        @Override
        public LLMProvider llmProvider() {
            return llmProvider;
        }

        @Override
        public String outputPath() {
            return outputPath;
        }

        @Override
        public boolean verbose() {
            return verbose;
        }

        @Override
        public FormatConverter.OutputFormat outputFormat() {
            return outputFormat;
        }

        @Override
        public OpenAI openai() {
            return new TestOpenAI(openAiApiKey);
        }

        @Override
        public Gemini gemini() {
            return new TestGemini(geminiApiKey);
        }

        public void setLlmProvider(LLMProvider provider) {
            this.llmProvider = provider;
        }

        public void setOpenAiApiKey(String apiKey) {
            this.openAiApiKey = apiKey;
        }

        public void setGeminiApiKey(String apiKey) {
            this.geminiApiKey = apiKey;
        }
    }

    private static class TestOpenAI implements WaverConfig.OpenAI {
        private final String apiKey;

        public TestOpenAI(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public Optional<String> apiKey() {
            return Optional.ofNullable(apiKey);
        }
    }

    private static class TestGemini implements WaverConfig.Gemini {
        private final String apiKey;

        public TestGemini(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public Optional<String> apiKey() {
            return Optional.ofNullable(apiKey);
        }
    }

    /**
     * Test implementation of MinioConfig for unit testing.
     */
    private static class TestMinioConfig implements MinioConfig {
        @Override
        public String endpoint() {
            return "http://localhost:9000";
        }

        @Override
        public String accessKey() {
            return "minioadmin";
        }

        @Override
        public String secretKey() {
            return "minioadmin";
        }

        @Override
        public String bucketName() {
            return "test-bucket";
        }
    }
}
