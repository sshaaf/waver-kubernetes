package dev.shaaf.waver.backend.config;

import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Configuration interface for MinIO object storage settings.
 * <p>
 * This interface uses Quarkus ConfigMapping to automatically bind
 * configuration properties with the "minio" prefix to the interface methods.
 * All configuration values are required and must be provided in the
 * application.properties file or through environment variables.
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationScoped
@ConfigMapping(prefix = "minio")
public interface MinioConfig {
    /**
     * Gets the MinIO server endpoint URL.
     *
     * @return The MinIO server endpoint (e.g., "http://localhost:9000")
     */
    String endpoint();

    /**
     * Gets the MinIO access key for authentication.
     *
     * @return The access key used to authenticate with MinIO
     */
    String accessKey();

    /**
     * Gets the MinIO secret key for authentication.
     *
     * @return The secret key used to authenticate with MinIO
     */
    String secretKey();

    /**
     * Gets the default MinIO bucket name for storing generated tutorials.
     *
     * @return The bucket name where tutorial files will be uploaded
     */
    String bucketName();

}
