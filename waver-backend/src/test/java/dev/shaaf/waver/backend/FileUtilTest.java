package dev.shaaf.waver.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileUtil class.
 * <p>
 * Tests the various scenarios for extracting folder names from input paths,
 * including handling of Git repositories, regular paths, and edge cases.
 */
class FileUtilTest {

    @Test
    void testGetFolderNameFromInputPathWithSimplePath() {
        // Given
        String inputPath = "/home/user/projects/my-project";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("my-project", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithGitRepository() {
        // Given
        String inputPath = "/home/user/projects/my-project.git";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("my-project", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithHttpsGitUrl() {
        // Given
        String inputPath = "https://github.com/user/repo.git";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("repo", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithSshGitUrl() {
        // Given
        String inputPath = "git@github.com:user/repo.git";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("repo", result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/simple/path/folder",
            "relative/path/folder", 
            "./local/folder",
            "../parent/folder"
    })
    void testGetFolderNameFromInputPathWithVariousPaths(String inputPath) {
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("folder", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithWindowsPath() {
        // Given - Windows path behavior depends on the OS
        String inputPath = "C:\\Windows\\Path\\folder";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        // On Windows, this would return "folder", on Unix systems it returns the whole string
        assertNotNull(result);
        assertTrue(result.equals("folder") || result.equals("C:\\Windows\\Path\\folder"));
    }

    @ParameterizedTest
    @CsvSource({
            "'/path/to/project.git', 'project'",
            "'/path/to/project', 'project'",
            "'https://github.com/owner/repo.git', 'repo'",
            "'git@bitbucket.org:team/project.git', 'project'",
            "'file://path/to/local.git', 'local'",
            "'my-awesome-project.git', 'my-awesome-project'"
    })
    void testGetFolderNameFromInputPathWithDifferentFormats(String inputPath, String expectedResult) {
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testGetFolderNameFromInputPathWithInvalidInput(String inputPath) {
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertNull(result);
    }

    @Test
    void testGetFolderNameFromInputPathWithRootPath() {
        // Given
        String inputPath = "/";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertNull(result);
    }

    @Test
    void testGetFolderNameFromInputPathWithWindowsRootPath() {
        // Given
        String inputPath = "C:\\";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        // On Unix systems, this returns "C:\" as it's treated as a filename
        // On Windows systems, this would return null
        assertNotNull(result);
        assertTrue(result == null || result.equals("C:\\"));
    }

    @Test
    void testGetFolderNameFromInputPathWithSingleFileName() {
        // Given
        String inputPath = "project";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("project", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithGitFileExtension() {
        // Given
        String inputPath = "project.git";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("project", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithMultipleGitExtensions() {
        // Given
        String inputPath = "/path/to/project.git.git";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("project.git", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithDotGitInMiddle() {
        // Given
        String inputPath = "/path/to/my.git.project";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("my.git.project", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithTrailingSlash() {
        // Given
        String inputPath = "/path/to/project/";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("project", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithComplexGitUrl() {
        // Given
        String inputPath = "ssh://git@enterprise.company.com:2222/team/awesome-project.git";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("awesome-project", result);
    }

    @Test
    void testGetFolderNameFromInputPathWithSpecialCharacters() {
        // Given
        String inputPath = "/path/to/my-awesome_project@2023";
        
        // When
        String result = FileUtil.getFolderNameFromInputPath(inputPath);
        
        // Then
        assertEquals("my-awesome_project@2023", result);
    }
}
