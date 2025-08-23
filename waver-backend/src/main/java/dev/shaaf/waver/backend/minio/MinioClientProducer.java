package dev.shaaf.waver.backend.minio;

import dev.shaaf.waver.backend.config.MinioConfig;
import io.minio.MinioClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * CDI producer for creating and configuring MinioClient instances.
 * <p>
 * This class is responsible for creating properly configured MinioClient
 * instances that can be injected throughout the application. The client
 * is configured using the MinioConfig settings.
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
@ApplicationScoped
public class MinioClientProducer {

    /** Injected MinIO configuration settings. */
    @Inject
    MinioConfig minioConfig;

    /**
     * Produces a configured MinioClient instance for CDI injection.
     * <p>
     * The client is configured with the endpoint URL and credentials
     * from the MinioConfig. This method is called once during application
     * startup to create a singleton MinioClient.
     *
     * @return A configured MinioClient instance ready for use
     */
    @Produces
    @ApplicationScoped
    public MinioClient getMinioClient() {
        return MinioClient.builder()
                .endpoint(minioConfig.endpoint())
                .credentials(minioConfig.accessKey(), minioConfig.secretKey())
                .build();
    }

}
