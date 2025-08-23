package dev.shaaf.waver.backend.minio;

import dev.shaaf.waver.backend.config.MinioConfig;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MinioClientProducer class.
 * <p>
 * Tests the CDI producer functionality for creating properly configured
 * MinioClient instances without using mocks.
 */
class MinioClientProducerTest {

    private MinioClientProducer producer;
    private TestMinioConfig minioConfig;

    @BeforeEach
    void setUp() {
        producer = new MinioClientProducer();
        minioConfig = new TestMinioConfig();
        producer.minioConfig = minioConfig;
    }

    @Test
    void testGetMinioClientCreatesValidInstance() {
        // When
        MinioClient client = producer.getMinioClient();
        
        // Then
        assertNotNull(client);
        assertInstanceOf(MinioClient.class, client);
    }

    @Test
    void testGetMinioClientWithValidConfiguration() {
        // Given
        minioConfig.setEndpoint("http://localhost:9000");
        minioConfig.setAccessKey("testAccessKey");
        minioConfig.setSecretKey("testSecretKey");
        
        // When
        MinioClient client = producer.getMinioClient();
        
        // Then
        assertNotNull(client);
        // Note: MinioClient doesn't expose getters for configuration,
        // so we can only verify that no exception is thrown during creation
    }

    @Test
    void testGetMinioClientWithDifferentEndpoints() {
        // Test with localhost
        minioConfig.setEndpoint("http://localhost:9000");
        MinioClient client1 = producer.getMinioClient();
        assertNotNull(client1);
        
        // Test with different port
        minioConfig.setEndpoint("http://localhost:9001");
        MinioClient client2 = producer.getMinioClient();
        assertNotNull(client2);
        
        // Test with remote endpoint
        minioConfig.setEndpoint("https://s3.amazonaws.com");
        MinioClient client3 = producer.getMinioClient();
        assertNotNull(client3);
    }

    @Test
    void testGetMinioClientWithHttpsEndpoint() {
        // Given
        minioConfig.setEndpoint("https://secure.minio.example.com");
        
        // When
        MinioClient client = producer.getMinioClient();
        
        // Then
        assertNotNull(client);
    }

    @Test
    void testGetMinioClientWithCustomPort() {
        // Given
        minioConfig.setEndpoint("http://minio.example.com:9090");
        
        // When
        MinioClient client = producer.getMinioClient();
        
        // Then
        assertNotNull(client);
    }

    @Test
    void testGetMinioClientConsistentBehavior() {
        // When
        MinioClient client1 = producer.getMinioClient();
        MinioClient client2 = producer.getMinioClient();
        
        // Then
        assertNotNull(client1);
        assertNotNull(client2);
        // Each call should create a new instance (unless caching is implemented)
        // This depends on the CDI scope configuration
    }

    @Test
    void testGetMinioClientWithSpecialCharactersInCredentials() {
        // Given
        minioConfig.setAccessKey("access+key/with=special&characters");
        minioConfig.setSecretKey("secret+key/with=special&characters");
        
        // When & Then
        assertDoesNotThrow(() -> {
            MinioClient client = producer.getMinioClient();
            assertNotNull(client);
        });
    }

    @Test
    void testGetMinioClientWithLongCredentials() {
        // Given
        String longAccessKey = "a".repeat(100);
        String longSecretKey = "s".repeat(100);
        minioConfig.setAccessKey(longAccessKey);
        minioConfig.setSecretKey(longSecretKey);
        
        // When & Then
        assertDoesNotThrow(() -> {
            MinioClient client = producer.getMinioClient();
            assertNotNull(client);
        });
    }

    @Test
    void testGetMinioClientDoesNotThrowWithValidConfig() {
        // When & Then
        assertDoesNotThrow(() -> {
            MinioClient client = producer.getMinioClient();
            assertNotNull(client);
        });
    }

    /**
     * Test implementation of MinioConfig for unit testing.
     */
    private static class TestMinioConfig implements MinioConfig {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "testAccessKey";
        private String secretKey = "testSecretKey";
        private String bucketName = "test-bucket";

        @Override
        public String endpoint() {
            return endpoint;
        }

        @Override
        public String accessKey() {
            return accessKey;
        }

        @Override
        public String secretKey() {
            return secretKey;
        }

        @Override
        public String bucketName() {
            return bucketName;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
    }
}
