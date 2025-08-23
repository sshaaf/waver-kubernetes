package dev.shaaf.waver.backend.minio;


import dev.shaaf.jgraphlet.PipelineContext;
import dev.shaaf.jgraphlet.Task;
import dev.shaaf.jgraphlet.TaskRunException;
import dev.shaaf.waver.backend.FileUtil;
import dev.shaaf.waver.llm.tutorial.model.GenerationContext;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Task implementation for uploading generated tutorial files to MinIO storage.
 * <p>
 * This task is part of the tutorial generation pipeline and handles the final
 * step of uploading all generated files to MinIO object storage. It recursively
 * uploads all files from a source directory while maintaining the directory structure.
 * <p>
 * The task implements the Task interface from the jgraphlet pipeline framework
 * and runs asynchronously as part of the overall generation process.
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
public class MinioUploaderTask implements Task<GenerationContext, UploadResult> {

    /** MinIO client for performing upload operations. */
    MinioClient minioClient;

    /** Source directory containing files to upload. */
    Path sourceDirectory;

    /** Target bucket name in MinIO. */
    String bucketName;

    /** Project name used as prefix for uploaded objects. */
    String uploadProjectName;

    /**
     * Constructs a new MinioUploaderTask with the specified parameters.
     *
     * @param minioClient The MinIO client for upload operations
     * @param sourceDirectory The directory containing files to upload
     * @param bucketName The target bucket name in MinIO
     */
    public MinioUploaderTask(MinioClient minioClient, Path sourceDirectory, String bucketName) {
        this.minioClient = minioClient;
        this.sourceDirectory = sourceDirectory;
        this.bucketName = bucketName;
        this.uploadProjectName = FileUtil.getFolderNameFromInputPath(sourceDirectory.toString());
    }

    /**
     * Executes the upload task asynchronously.
     * <p>
     * This method is called by the pipeline framework and performs the
     * actual upload operation in a separate thread.
     *
     * @param generationContext The generation context (not used in this task)
     * @param context The pipeline context for accessing shared data
     * @return A CompletableFuture containing the upload result
     * @throws TaskRunException if the upload operation fails
     */
    @Override
    public CompletableFuture<UploadResult> execute(GenerationContext generationContext, PipelineContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return uploadDirectory(sourceDirectory.resolve(this.sourceDirectory), bucketName);
            } catch (Exception e) {
                e.printStackTrace();
                throw new TaskRunException("Failed to upload to MinIO", e);
            }
        });
    }

    /**
     * Uploads all files from the source directory to MinIO recursively.
     * <p>
     * This method walks through the entire directory tree and uploads each
     * regular file to MinIO while preserving the directory structure. The
     * upload process tracks successful and failed uploads.
     *
     * @param sourceDirectory The source directory to upload
     * @param bucketName The target bucket name
     * @return An UploadResult containing lists of successful and failed uploads
     * @throws IllegalArgumentException if the source directory doesn't exist
     * @throws RuntimeException if directory traversal fails
     */
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
                            minioClient.uploadObject(
                                    UploadObjectArgs.builder()
                                            .bucket(bucketName)
                                            .object(objectName)
                                            .filename(filePath.toString())
                                            .build());

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


