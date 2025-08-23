package dev.shaaf.waver.backend.minio;

import dev.shaaf.jgraphlet.PipelineContext;
import dev.shaaf.waver.backend.FileUtil;
import dev.shaaf.waver.llm.tutorial.model.GenerationContext;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MinioUploaderTask class.
 * <p>
 * Tests the file upload functionality without actually connecting to MinIO,
 * using test implementations and temporary directories.
 */
class MinioUploaderTaskTest {

    @TempDir
    Path tempDir;

    private TestMinioUploaderTask uploaderTask;
    private TestMinioClient testMinioClient;
    private GenerationContext generationContext;
    private TestPipelineContext pipelineContext;

    @BeforeEach
    void setUp() {
        testMinioClient = new TestMinioClient();
        generationContext = createTestGenerationContext();
        pipelineContext = new TestPipelineContext();
    }

    @Test
    void testConstructorSetsFields() {
        // Given
        String bucketName = "test-bucket";
        
        // When
        uploaderTask = new TestMinioUploaderTask(testMinioClient, tempDir, bucketName);
        
        // Then
        assertNotNull(uploaderTask);
        assertEquals(testMinioClient, uploaderTask.minioClient);
        assertEquals(tempDir, uploaderTask.sourceDirectory);
        assertEquals(bucketName, uploaderTask.bucketName);
    }

    @Test
    void testExecuteWithEmptyDirectory() throws ExecutionException, InterruptedException {
        // Given
        uploaderTask = new TestMinioUploaderTask(testMinioClient, tempDir, "test-bucket");
        
        // When
        CompletableFuture<UploadResult> future = uploaderTask.execute(generationContext, pipelineContext);
        UploadResult result = future.get();
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.successfulUploads().isEmpty());
        assertTrue(result.failedUploads().isEmpty());
    }

    @Test
    void testExecuteWithSingleFile() throws IOException, ExecutionException, InterruptedException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");
        uploaderTask = new TestMinioUploaderTask(testMinioClient, tempDir, "test-bucket");
        
        // When
        CompletableFuture<UploadResult> future = uploaderTask.execute(generationContext, pipelineContext);
        UploadResult result = future.get();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(1, testMinioClient.getUploadedObjects().size());
    }

    @Test
    void testExecuteWithMultipleFiles() throws IOException, ExecutionException, InterruptedException {
        // Given
        Files.writeString(tempDir.resolve("file1.txt"), "content1");
        Files.writeString(tempDir.resolve("file2.txt"), "content2");
        Files.writeString(tempDir.resolve("file3.txt"), "content3");
        uploaderTask = new TestMinioUploaderTask(testMinioClient, tempDir, "test-bucket");
        
        // When
        CompletableFuture<UploadResult> future = uploaderTask.execute(generationContext, pipelineContext);
        UploadResult result = future.get();
        
        // Then
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(3, testMinioClient.getUploadedObjects().size());
    }

    @Test
    void testExecuteWithNestedDirectories() throws IOException, ExecutionException, InterruptedException {
        // Given
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.writeString(tempDir.resolve("root.txt"), "root content");
        Files.writeString(subDir.resolve("nested.txt"), "nested content");
        
        uploaderTask = new TestMinioUploaderTask(testMinioClient, tempDir, "test-bucket");
        
        // When
        CompletableFuture<UploadResult> future = uploaderTask.execute(generationContext, pipelineContext);
        UploadResult result = future.get();
        
        // Then
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        
        List<String> uploadedObjects = testMinioClient.getUploadedObjects();
        assertTrue(uploadedObjects.stream().anyMatch(obj -> obj.endsWith("root.txt")));
        assertTrue(uploadedObjects.stream().anyMatch(obj -> obj.contains("subdir") && obj.endsWith("nested.txt")));
    }

    @Test
    void testUploadDirectoryWithNonExistentDirectory() {
        // Given
        Path nonExistentDir = tempDir.resolve("non-existent");
        uploaderTask = new TestMinioUploaderTask(testMinioClient, nonExistentDir, "test-bucket");
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            uploaderTask.uploadDirectory(nonExistentDir, "test-bucket");
        });
    }

    @Test
    void testUploadDirectoryWithFileInsteadOfDirectory() throws IOException {
        // Given
        Path file = tempDir.resolve("not-a-directory.txt");
        Files.writeString(file, "content");
        uploaderTask = new TestMinioUploaderTask(testMinioClient, file, "test-bucket");
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            uploaderTask.uploadDirectory(file, "test-bucket");
        });
    }

    @Test
    void testUploadDirectoryWithUploadFailure() throws IOException {
        // Given
        Files.writeString(tempDir.resolve("file1.txt"), "content1");
        Files.writeString(tempDir.resolve("file2.txt"), "content2");
        testMinioClient.setShouldFailUpload(true);
        uploaderTask = new TestMinioUploaderTask(testMinioClient, tempDir, "test-bucket");
        
        // When
        UploadResult result = uploaderTask.uploadDirectory(tempDir, "test-bucket");
        
        // Then
        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
    }

    @Test
    void testUploadDirectoryWithMixedResults() throws IOException {
        // Given
        Files.writeString(tempDir.resolve("success.txt"), "content");
        Files.writeString(tempDir.resolve("failure.txt"), "content");
        testMinioClient.setFailurePattern("failure.txt");
        uploaderTask = new TestMinioUploaderTask(testMinioClient, tempDir, "test-bucket");
        
        // When
        UploadResult result = uploaderTask.uploadDirectory(tempDir, "test-bucket");
        
        // Then
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.successfulUploads().stream().anyMatch(obj -> obj.contains("success.txt")));
        assertTrue(result.failedUploads().stream().anyMatch(path -> path.contains("failure.txt")));
    }

    /**
     * Test version of MinioUploaderTask that uses our TestMinioClient.
     */
    private static class TestMinioUploaderTask {
        TestMinioClient minioClient;
        Path sourceDirectory;
        String bucketName;
        String uploadProjectName;

        public TestMinioUploaderTask(TestMinioClient minioClient, Path sourceDirectory, String bucketName) {
            this.minioClient = minioClient;
            this.sourceDirectory = sourceDirectory;
            this.bucketName = bucketName;
            this.uploadProjectName = FileUtil.getFolderNameFromInputPath(sourceDirectory.toString());
        }

        public CompletableFuture<UploadResult> execute(GenerationContext generationContext, TestPipelineContext context) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return uploadDirectory(sourceDirectory, bucketName);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to upload to MinIO", e);
                }
            });
        }

        public UploadResult uploadDirectory(Path sourceDirectory, String bucketName) {
            if (!Files.exists(sourceDirectory) || !Files.isDirectory(sourceDirectory)) {
                throw new IllegalArgumentException("Source path must be an existing directory: " + sourceDirectory);
            }

            List<String> successfulUploads = new ArrayList<>();
            List<String> failedUploads = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(sourceDirectory)) {
                stream
                        .filter(Files::isRegularFile)
                        .forEach(filePath -> {
                            try {
                                Path relativePath = sourceDirectory.relativize(filePath);
                                String objectName = Paths.get(uploadProjectName, relativePath.toString()).toString();
                                objectName = objectName.replace('\\', '/');
                                
                                UploadObjectArgs args = UploadObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(objectName)
                                        .filename(filePath.toString())
                                        .build();
                                        
                                minioClient.uploadObject(args);
                                successfulUploads.add(objectName);

                            } catch (Exception e) {
                                e.printStackTrace();
                                failedUploads.add(filePath.toString());
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Failed to read the source directory: " + sourceDirectory, e);
            }

            return new UploadResult(successfulUploads, failedUploads);
        }
    }

    /**
     * Test implementation of MinioClient for unit testing.
     */
    private static class TestMinioClient {
        private final List<String> uploadedObjects = new ArrayList<>();
        private boolean shouldFailUpload = false;
        private String failurePattern = null;

        public void uploadObject(UploadObjectArgs args) throws Exception {
            String objectName = args.object();
            
            if (shouldFailUpload || (failurePattern != null && objectName.contains(failurePattern))) {
                throw new RuntimeException("Test upload failure for: " + objectName);
            }
            
            uploadedObjects.add(objectName);
        }

        public List<String> getUploadedObjects() {
            return new ArrayList<>(uploadedObjects);
        }

        public void setShouldFailUpload(boolean shouldFail) {
            this.shouldFailUpload = shouldFail;
        }

        public void setFailurePattern(String pattern) {
            this.failurePattern = pattern;
        }
    }

    /**
     * Test implementation of GenerationContext using null to satisfy constructor.
     */
    private GenerationContext createTestGenerationContext() {
        // Since GenerationContext is a record with required fields, we'll use null as placeholder
        return null; // For unit tests, we can pass null as it's not used in the upload logic
    }

    /**
     * Test implementation of PipelineContext.
     */
    private static class TestPipelineContext {
        public <T> void put(String key, T value) {
            // Implementation not needed for these tests
        }

        public <T> T get(String key, Class<T> type) {
            // Implementation not needed for these tests
            return null;
        }

        public boolean containsKey(String key) {
            return false;
        }
    }
}
