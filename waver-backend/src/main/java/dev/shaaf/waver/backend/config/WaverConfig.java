package dev.shaaf.waver.backend.config;


import dev.shaaf.waver.llm.config.FormatConverter;
import dev.shaaf.waver.llm.config.LLMProvider;
import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Main configuration interface for Waver application settings.
 * <p>
 * This interface uses Quarkus ConfigMapping to automatically bind
 * configuration properties with the "waver" prefix. It includes
 * settings for LLM providers, output configurations, and nested
 * provider-specific configurations.
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationScoped
@ConfigMapping(prefix = "waver")
public interface WaverConfig {
    /**
     * Gets the configured Language Learning Model provider.
     *
     * @return The LLM provider (e.g., OpenAI, Gemini)
     */
    LLMProvider llmProvider();

    /**
     * Gets the file system path where generated tutorials should be saved.
     *
     * @return The output directory path for generated content
     */
    String outputPath();

    /**
     * Gets the verbose logging configuration.
     *
     * @return true if verbose logging is enabled, false otherwise
     */
    boolean verbose();

    /**
     * Gets the output format for generated tutorials.
     *
     * @return The output format (e.g., MARKDOWN, HTML)
     */
    FormatConverter.OutputFormat outputFormat();

    /**
     * Gets the OpenAI-specific configuration.
     *
     * @return The OpenAI configuration containing API keys and settings
     */
    OpenAI openai();

    /**
     * Gets the Gemini-specific configuration.
     *
     * @return The Gemini configuration containing API keys and settings
     */
    Gemini gemini();

    /**
     * Nested configuration interface for OpenAI-specific settings.
     */
    interface OpenAI {
        /**
         * Gets the OpenAI API key.
         *
         * @return Optional containing the API key if configured
         */
        Optional<String> apiKey();
    }

    /**
     * Nested configuration interface for Gemini-specific settings.
     */
    interface Gemini {
        /**
         * Gets the Gemini API key.
         *
         * @return Optional containing the API key if configured
         */
        Optional<String> apiKey();
    }
}