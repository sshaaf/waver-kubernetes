package dev.shaaf.waver.backend.minio;

import dev.shaaf.waver.backend.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * Initializes MinIO buckets at application startup.
 * <p>
 * This class ensures that the required MinIO buckets exist before the
 * application starts processing requests. It automatically creates any
 * missing buckets based on the configuration.
 * <p>
 * The @Startup annotation ensures this initialization happens early
 * in the application lifecycle.
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
@Startup
public class MinioBucketInitializer {

    /** Injected MinIO client for bucket operations. */
    @Inject
    MinioClient minioClient;

    /** Injected MinIO configuration containing bucket settings. */
    @Inject
    MinioConfig minioConfig;


    /**
     * Initializes MinIO buckets after dependency injection is complete.
     * <p>
     * This method checks if the configured bucket exists and creates it
     * if it doesn't. Any errors during bucket creation are logged but
     * don't prevent application startup.
     *
     * @throws RuntimeException if bucket operations fail critically
     */
    @PostConstruct
    public void initBuckets() {
        boolean found = false;
        try {
            found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.bucketName()).build());

            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.bucketName()).build());
            } else {
                Log.infof("Bucket already exists: " + minioConfig.bucketName());
            }
        } catch (Exception e) {
            Log.error("Could not initialize bucket", e);
        }
    }

}
