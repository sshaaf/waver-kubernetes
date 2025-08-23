package dev.shaaf.waver.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WaverProcessEvent record.
 * <p>
 * Tests the creation, equality, and functionality of the WaverProcessEvent record
 * which represents tutorial generation processing events.
 */
class WaverProcessEventTest {

    @Test
    void testCreateWaverProcessEventWithValidUrl() {
        // Given
        String sourceUrl = "https://github.com/user/repo.git";
        
        // When
        WaverProcessEvent event = new WaverProcessEvent(sourceUrl);
        
        // Then
        assertNotNull(event);
        assertEquals(sourceUrl, event.sourceUrl());
    }

    @Test
    void testCreateWaverProcessEventWithNullUrl() {
        // When
        WaverProcessEvent event = new WaverProcessEvent(null);
        
        // Then
        assertNotNull(event);
        assertNull(event.sourceUrl());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/user/repo.git",
            "git@github.com:user/repo.git",
            "/local/path/to/repo",
            "file:///absolute/path/to/repo",
            "ssh://git@server.com/repo.git",
            "relative/path/to/repo"
    })
    void testCreateWaverProcessEventWithDifferentSourceUrls(String sourceUrl) {
        // When
        WaverProcessEvent event = new WaverProcessEvent(sourceUrl);
        
        // Then
        assertNotNull(event);
        assertEquals(sourceUrl, event.sourceUrl());
    }

    @Test
    void testWaverProcessEventEquality() {
        // Given
        String sourceUrl = "https://github.com/user/repo.git";
        WaverProcessEvent event1 = new WaverProcessEvent(sourceUrl);
        WaverProcessEvent event2 = new WaverProcessEvent(sourceUrl);
        
        // When & Then
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testWaverProcessEventInequalityWithDifferentUrls() {
        // Given
        WaverProcessEvent event1 = new WaverProcessEvent("https://github.com/user/repo1.git");
        WaverProcessEvent event2 = new WaverProcessEvent("https://github.com/user/repo2.git");
        
        // When & Then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testWaverProcessEventEqualityWithNullUrls() {
        // Given
        WaverProcessEvent event1 = new WaverProcessEvent(null);
        WaverProcessEvent event2 = new WaverProcessEvent(null);
        
        // When & Then
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testWaverProcessEventInequalityWithOneNullUrl() {
        // Given
        WaverProcessEvent event1 = new WaverProcessEvent("https://github.com/user/repo.git");
        WaverProcessEvent event2 = new WaverProcessEvent(null);
        
        // When & Then
        assertNotEquals(event1, event2);
    }

    @Test
    void testWaverProcessEventToString() {
        // Given
        String sourceUrl = "https://github.com/user/repo.git";
        WaverProcessEvent event = new WaverProcessEvent(sourceUrl);
        
        // When
        String toString = event.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("WaverProcessEvent"));
        assertTrue(toString.contains(sourceUrl));
    }

    @Test
    void testWaverProcessEventToStringWithNullUrl() {
        // Given
        WaverProcessEvent event = new WaverProcessEvent(null);
        
        // When
        String toString = event.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("WaverProcessEvent"));
        assertTrue(toString.contains("null"));
    }

    @Test
    void testWaverProcessEventWithEmptyString() {
        // Given
        String sourceUrl = "";
        
        // When
        WaverProcessEvent event = new WaverProcessEvent(sourceUrl);
        
        // Then
        assertNotNull(event);
        assertEquals("", event.sourceUrl());
    }

    @Test
    void testWaverProcessEventWithWhitespaceUrl() {
        // Given
        String sourceUrl = "   https://github.com/user/repo.git   ";
        
        // When
        WaverProcessEvent event = new WaverProcessEvent(sourceUrl);
        
        // Then
        assertNotNull(event);
        assertEquals(sourceUrl, event.sourceUrl());
    }

    @Test
    void testWaverProcessEventImmutability() {
        // Given
        String originalUrl = "https://github.com/user/repo.git";
        WaverProcessEvent event = new WaverProcessEvent(originalUrl);
        
        // When
        String retrievedUrl = event.sourceUrl();
        
        // Then
        assertEquals(originalUrl, retrievedUrl);
        // Verify that the record is immutable (no setters exist)
        // This is guaranteed by the record structure itself
        assertSame(originalUrl, retrievedUrl);
    }
}
