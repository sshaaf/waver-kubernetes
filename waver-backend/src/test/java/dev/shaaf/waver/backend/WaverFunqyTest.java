package dev.shaaf.waver.backend;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WaverFunqy class.
 * <p>
 * Tests the Funqy endpoint functionality for processing tutorial generation requests
 * without using mocks, instead using test implementations.
 */
class WaverFunqyTest {

    private WaverFunqy waverFunqy;
    private TestEmitter<WaverProcessEvent> testEmitter;

    @BeforeEach
    void setUp() {
        waverFunqy = new WaverFunqy();
        testEmitter = new TestEmitter<>();
        waverFunqy.requestEmitter = testEmitter;
    }

    @Test
    void testGenerateWithValidRequest() {
        // Given
        WaverProcessEvent request = new WaverProcessEvent("https://github.com/user/repo.git");
        
        // When
        waverFunqy.generate(request);
        
        // Then
        assertEquals(1, testEmitter.getSentMessages().size());
        assertEquals(request, testEmitter.getSentMessages().get(0));
        assertFalse(testEmitter.hasErrors());
    }

    @Test
    void testGenerateWithNullRequest() {
        // When
        waverFunqy.generate(null);
        
        // Then
        assertEquals(0, testEmitter.getSentMessages().size());
        assertFalse(testEmitter.hasErrors());
    }

    @Test
    void testGenerateWithRequestWithNullSourceUrl() {
        // Given
        WaverProcessEvent request = new WaverProcessEvent(null);
        
        // When
        waverFunqy.generate(request);
        
        // Then
        assertEquals(0, testEmitter.getSentMessages().size());
        assertFalse(testEmitter.hasErrors());
    }

    @Test
    void testGenerateWithMultipleRequests() {
        // Given
        WaverProcessEvent request1 = new WaverProcessEvent("https://github.com/user/repo1.git");
        WaverProcessEvent request2 = new WaverProcessEvent("https://github.com/user/repo2.git");
        
        // When
        waverFunqy.generate(request1);
        waverFunqy.generate(request2);
        
        // Then
        assertEquals(2, testEmitter.getSentMessages().size());
        assertTrue(testEmitter.getSentMessages().contains(request1));
        assertTrue(testEmitter.getSentMessages().contains(request2));
    }

    @Test
    void testGenerateWithEmptySourceUrl() {
        // Given
        WaverProcessEvent request = new WaverProcessEvent("");
        
        // When
        waverFunqy.generate(request);
        
        // Then
        // The actual implementation sends even empty strings through
        assertEquals(1, testEmitter.getSentMessages().size());
        assertEquals(request, testEmitter.getSentMessages().get(0));
    }

    @Test
    void testGenerateWithWhitespaceOnlySourceUrl() {
        // Given
        WaverProcessEvent request = new WaverProcessEvent("   \t\n   ");
        
        // When
        waverFunqy.generate(request);
        
        // Then
        // The actual implementation sends even whitespace-only strings through
        assertEquals(1, testEmitter.getSentMessages().size());
        assertEquals(request, testEmitter.getSentMessages().get(0));
    }

    @Test
    void testGenerateWithValidLocalPath() {
        // Given
        WaverProcessEvent request = new WaverProcessEvent("/local/path/to/repo");
        
        // When
        waverFunqy.generate(request);
        
        // Then
        assertEquals(1, testEmitter.getSentMessages().size());
        assertEquals(request, testEmitter.getSentMessages().get(0));
    }

    @Test
    void testGenerateWithSshGitUrl() {
        // Given
        WaverProcessEvent request = new WaverProcessEvent("git@github.com:user/repo.git");
        
        // When
        waverFunqy.generate(request);
        
        // Then
        assertEquals(1, testEmitter.getSentMessages().size());
        assertEquals(request, testEmitter.getSentMessages().get(0));
    }

    @Test
    void testGenerateDoesNotThrowException() {
        // Given
        WaverProcessEvent validRequest = new WaverProcessEvent("https://github.com/user/repo.git");
        WaverProcessEvent invalidRequest = new WaverProcessEvent(null);
        
        // When & Then
        assertDoesNotThrow(() -> waverFunqy.generate(validRequest));
        assertDoesNotThrow(() -> waverFunqy.generate(invalidRequest));
        assertDoesNotThrow(() -> waverFunqy.generate(null));
    }

    /**
     * Test implementation of Emitter for unit testing.
     */
    private static class TestEmitter<T> implements Emitter<T> {
        private final List<T> sentMessages = new ArrayList<>();
        private boolean hasErrors = false;

        @Override
        public CompletableFuture<Void> send(T msg) {
            sentMessages.add(msg);
            return CompletableFuture.completedFuture(null);
        }

        public void send(T msg, @SuppressWarnings("rawtypes") org.eclipse.microprofile.reactive.messaging.Metadata metadata) {
            sentMessages.add(msg);
        }

        @Override
        public <M extends org.eclipse.microprofile.reactive.messaging.Message<? extends T>> void send(M msg) {
            if (msg.getPayload() != null) {
                sentMessages.add(msg.getPayload());
            }
        }

        @Override
        public void error(Exception e) {
            hasErrors = true;
        }

        @Override
        public void complete() {
            // Implementation not needed for this test
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean hasRequests() {
            return true;
        }

        public List<T> getSentMessages() {
            return new ArrayList<>(sentMessages);
        }

        public boolean hasErrors() {
            return hasErrors;
        }
    }
}
