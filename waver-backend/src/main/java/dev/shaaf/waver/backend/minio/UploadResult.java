package dev.shaaf.waver.backend.minio;

import java.util.List;

/**
 * Record representing the result of a MinIO upload operation.
 * <p>
 * This immutable data structure contains the results of uploading
 * multiple files to MinIO storage, tracking both successful and
 * failed upload attempts.
 *
 * @param successfulUploads List of object names that were successfully uploaded
 * @param failedUploads List of file paths that failed to upload
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
public record UploadResult(List<String> successfulUploads, List<String> failedUploads) {

    /**
     * Gets the number of files that were successfully uploaded.
     *
     * @return The count of successful uploads
     */
    public int getSuccessCount() {
        return successfulUploads.size();
    }

    /**
     * Gets the number of files that failed to upload.
     *
     * @return The count of failed uploads
     */
    public int getFailureCount() {
        return failedUploads.size();
    }
}