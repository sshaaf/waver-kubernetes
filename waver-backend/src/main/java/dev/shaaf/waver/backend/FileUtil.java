package dev.shaaf.waver.backend;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class providing file and path manipulation operations.
 * <p>
 * This class contains static helper methods for common file operations
 * used throughout the Waver backend application.
 *
 * @author Waver Team
 * @version 1.0
 * @since 1.0
 */
public class FileUtil {

    /**
     * Extracts the folder name from a given input path string.
     * <p>
     * This method handles both regular directory paths and Git repository URLs.
     * If the path ends with ".git", the ".git" suffix is removed from the result.
     *
     * @param pathString The input path string to extract the folder name from
     * @return The folder name extracted from the path, or null if the input is invalid
     *         or if the path has no filename component
     *
     * @throws RuntimeException if the path cannot be parsed
     *
     * @see Path#getFileName()
     * @see Paths#get(String, String...)
     */
    public static String getFolderNameFromInputPath(String pathString) {
        if (pathString == null || pathString.trim().isEmpty()) {
            return null;
        }
        Path path = Paths.get(pathString);
        Path fileNamePath = path.getFileName();
        if (fileNamePath == null) {
            return null;
        }

        String projectName = fileNamePath.toString();
        if (projectName.endsWith(".git")) {
            return projectName.substring(0, projectName.length() - 4);
        } else {
            return projectName;
        }
    }
}
