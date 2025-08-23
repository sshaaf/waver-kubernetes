package dev.shaaf.waver.backend;

/**
 * Record representing a tutorial generation processing event.
 * <p>
 * This immutable data structure carries the necessary information for
 * triggering a tutorial generation process. It is used in the messaging
 * system to communicate between different components of the application.
 *
 * @param sourceUrl The URL or path to the source code repository from which
 *                 to generate tutorials
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
public record WaverProcessEvent(String sourceUrl) {

}
