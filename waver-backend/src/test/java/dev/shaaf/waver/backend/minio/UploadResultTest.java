package dev.shaaf.waver.backend.minio;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UploadResult record.
 * <p>
 * Tests the functionality and behavior of the UploadResult record
 * which tracks successful and failed MinIO upload operations.
 */
class UploadResultTest {

    @Test
    void testCreateUploadResultWithSuccessfulUploads() {
        // Given
        List<String> successful = Arrays.asList("file1.txt", "file2.txt", "file3.txt");
        List<String> failed = Collections.emptyList();
        
        // When
        UploadResult result = new UploadResult(successful, failed);
        
        // Then
        assertNotNull(result);
        assertEquals(successful, result.successfulUploads());
        assertEquals(failed, result.failedUploads());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    void testCreateUploadResultWithFailedUploads() {
        // Given
        List<String> successful = Collections.emptyList();
        List<String> failed = Arrays.asList("error1.txt", "error2.txt");
        
        // When
        UploadResult result = new UploadResult(successful, failed);
        
        // Then
        assertNotNull(result);
        assertEquals(successful, result.successfulUploads());
        assertEquals(failed, result.failedUploads());
        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
    }

    @Test
    void testCreateUploadResultWithMixedResults() {
        // Given
        List<String> successful = Arrays.asList("success1.txt", "success2.txt");
        List<String> failed = Arrays.asList("failed1.txt");
        
        // When
        UploadResult result = new UploadResult(successful, failed);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.successfulUploads().contains("success1.txt"));
        assertTrue(result.successfulUploads().contains("success2.txt"));
        assertTrue(result.failedUploads().contains("failed1.txt"));
    }

    @Test
    void testCreateUploadResultWithEmptyLists() {
        // Given
        List<String> successful = Collections.emptyList();
        List<String> failed = Collections.emptyList();
        
        // When
        UploadResult result = new UploadResult(successful, failed);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.successfulUploads().isEmpty());
        assertTrue(result.failedUploads().isEmpty());
    }

    @Test
    void testUploadResultEquality() {
        // Given
        List<String> successful = Arrays.asList("file1.txt", "file2.txt");
        List<String> failed = Arrays.asList("error.txt");
        UploadResult result1 = new UploadResult(successful, failed);
        UploadResult result2 = new UploadResult(successful, failed);
        
        // When & Then
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testUploadResultInequality() {
        // Given
        List<String> successful1 = Arrays.asList("file1.txt");
        List<String> failed1 = Arrays.asList("error1.txt");
        List<String> successful2 = Arrays.asList("file2.txt");
        List<String> failed2 = Arrays.asList("error2.txt");
        
        UploadResult result1 = new UploadResult(successful1, failed1);
        UploadResult result2 = new UploadResult(successful2, failed2);
        
        // When & Then
        assertNotEquals(result1, result2);
    }

    @Test
    void testUploadResultToString() {
        // Given
        List<String> successful = Arrays.asList("success.txt");
        List<String> failed = Arrays.asList("failure.txt");
        UploadResult result = new UploadResult(successful, failed);
        
        // When
        String toString = result.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("UploadResult"));
        assertTrue(toString.contains("success.txt"));
        assertTrue(toString.contains("failure.txt"));
    }

    @Test
    void testUploadResultWithNullLists() {
        // When & Then
        assertDoesNotThrow(() -> {
            UploadResult result = new UploadResult(null, null);
            assertNull(result.successfulUploads());
            assertNull(result.failedUploads());
        });
    }

    @Test
    void testGetSuccessCountWithNullList() {
        // Given
        UploadResult result = new UploadResult(null, Collections.emptyList());
        
        // When & Then
        assertThrows(NullPointerException.class, result::getSuccessCount);
    }

    @Test
    void testGetFailureCountWithNullList() {
        // Given
        UploadResult result = new UploadResult(Collections.emptyList(), null);
        
        // When & Then
        assertThrows(NullPointerException.class, result::getFailureCount);
    }

    @Test
    void testUploadResultWithLargeNumbers() {
        // Given
        List<String> successful = Arrays.asList(
            "file001.txt", "file002.txt", "file003.txt", "file004.txt", "file005.txt",
            "file006.txt", "file007.txt", "file008.txt", "file009.txt", "file010.txt"
        );
        List<String> failed = Arrays.asList("error001.txt", "error002.txt", "error003.txt");
        
        // When
        UploadResult result = new UploadResult(successful, failed);
        
        // Then
        assertEquals(10, result.getSuccessCount());
        assertEquals(3, result.getFailureCount());
        assertEquals(successful.size(), result.getSuccessCount());
        assertEquals(failed.size(), result.getFailureCount());
    }

    @Test
    void testUploadResultImmutability() {
        // Given
        List<String> successful = Arrays.asList("file1.txt", "file2.txt");
        List<String> failed = Arrays.asList("error.txt");
        UploadResult result = new UploadResult(successful, failed);
        
        // When
        List<String> retrievedSuccessful = result.successfulUploads();
        List<String> retrievedFailed = result.failedUploads();
        
        // Then
        // The record itself is immutable, but the lists could be modified
        // In a real implementation, you might want to make defensive copies
        assertSame(successful, retrievedSuccessful);
        assertSame(failed, retrievedFailed);
    }

    @Test
    void testUploadResultWithDuplicateEntries() {
        // Given
        List<String> successful = Arrays.asList("file1.txt", "file1.txt", "file2.txt");
        List<String> failed = Arrays.asList("error.txt", "error.txt");
        
        // When
        UploadResult result = new UploadResult(successful, failed);
        
        // Then
        assertEquals(3, result.getSuccessCount()); // Including duplicates
        assertEquals(2, result.getFailureCount()); // Including duplicates
    }
}
