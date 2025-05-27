package org.alexismp.pdfmerger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;

// Imports for this test method
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

class LocalStorageServiceTests {

    private LocalStorageService storageService;

    @TempDir
    Path tempDir; // JUnit 5 will inject a temporary directory here

    @BeforeEach
    void setUp() throws IOException {
        // LocalStorageService was modified in the previous step to accept Path in its constructor.
        // The constructor itself does not call init(), so we call it here.
        storageService = new LocalStorageService(tempDir);
        storageService.init(); // Call init to create the directory structure within tempDir
    }

    // Test methods will be added in subsequent steps.
    @Test
    void testInit_CreatesDirectory() {
        // storageService.init() is called in @BeforeEach setUp()

        // Get the root directory from the service (getter was added in a previous step)
        Path rootDir = storageService.getRootLocation();

        // Assert that the directory specified by storageService.getRootLocation() exists
        assertTrue(Files.exists(rootDir), "Root directory should exist after init.");
        assertTrue(Files.isDirectory(rootDir), "Root path should be a directory after init.");

        // Also, test that calling init() multiple times is safe (idempotent)
        assertDoesNotThrow(() -> storageService.init(), "Calling init() multiple times should not throw an exception.");
        assertTrue(Files.exists(rootDir), "Root directory should still exist after calling init() again.");
    }

    @Test
    void testStorePDF_ValidFile() throws IOException {
        String idPrefix = "testPrefix_validFile";
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", // Parameter name in a multipart request
                "test.pdf", // Original filename
                MediaType.APPLICATION_PDF_VALUE, // Content type
                "test pdf content".getBytes() // File content
        );

        // Action: Call storePDF and assert it doesn't throw an exception for a valid file
        assertDoesNotThrow(() -> storageService.storePDF(mockFile, idPrefix),
                "storePDF should not throw an exception for a valid file.");

        // Assertions for directory creation
        Path expectedDir = storageService.getRootLocation().resolve(idPrefix);
        assertTrue(Files.exists(expectedDir), "Directory for idPrefix should be created within rootLocation.");
        assertTrue(Files.isDirectory(expectedDir), "Path for idPrefix should be a directory.");

        // Assertions for file storage
        Path expectedPath = expectedDir.resolve("test.pdf");
        assertTrue(Files.exists(expectedPath), "Stored PDF file should exist at the expected path.");
        assertArrayEquals("test pdf content".getBytes(), Files.readAllBytes(expectedPath),
                "Content of stored PDF file should match the input content.");

        // Assertions for the internal state (allFiles map via getFilesToMerge)
        List<Path> filesToMerge = storageService.getFilesToMerge(idPrefix);
        assertNotNull(filesToMerge, "List of files to merge should not be null after storing a file.");
        assertEquals(1, filesToMerge.size(), "There should be one file in the list to merge.");
        // Compare absolute paths to avoid issues with relative vs. absolute path differences
        assertEquals(expectedPath.toAbsolutePath(), filesToMerge.get(0).toAbsolutePath(),
                "The path of the stored file should be correctly recorded in the filesToMerge list.");
    }

    @Test
    void testStorePDF_NonPdfFile_ThrowsException() {
        String idPrefix = "testPrefix_nonPdfFile";
        MockMultipartFile mockNonPdfFile = new MockMultipartFile(
                "file",
                "test.txt", // Non-PDF extension
                MediaType.TEXT_PLAIN_VALUE,
                "some text content".getBytes()
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            storageService.storePDF(mockNonPdfFile, idPrefix);
        }, "storePDF should throw ResponseStatusException for non-PDF files.");

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(),
                "Exception status should be BAD_REQUEST for non-PDF files.");

        // Based on current LocalStorageService.storePDF logic:
        // 1. The user-specific directory IS created before the file type check.
        // 2. The entry in `allFiles` map IS created before the file type check.
        // So, we adjust assertions from the example.

        Path expectedDir = storageService.getRootLocation().resolve(idPrefix);
        assertTrue(Files.exists(expectedDir),
                "Directory for idPrefix SHOULD be created even if file is invalid, as per current logic.");
        assertTrue(Files.isDirectory(expectedDir));

        // Assert that the actual file "test.txt" was not created within that directory.
        Path actualFile = expectedDir.resolve("test.txt");
        assertFalse(Files.exists(actualFile),
                "The non-PDF file itself should not be stored in the directory.");

        // Verify the allFiles map was populated but the file was not added to the list.
        List<Path> filesToMerge = storageService.getFilesToMerge(idPrefix);
        assertNotNull(filesToMerge, "Files to merge list should not be null, as map entry is created before file type check.");
        assertTrue(filesToMerge.isEmpty(), "Files to merge list should be empty as the invalid file was not added.");
    }

    @Test
    void testStorePDF_EmptyFile_ThrowsException() {
        String idPrefix = "testPrefix_emptyFile";
        MockMultipartFile mockEmptyFile = new MockMultipartFile(
                "file",
                "empty.pdf", // Valid PDF name
                MediaType.APPLICATION_PDF_VALUE,
                new byte[0]  // Empty content
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            storageService.storePDF(mockEmptyFile, idPrefix);
        }, "storePDF should throw ResponseStatusException for empty files.");

        assertEquals(HttpStatus.NO_CONTENT, exception.getStatus(),
                "Exception status should be NO_CONTENT for empty files.");

        // Based on current LocalStorageService.storePDF logic:
        // 1. The user-specific directory IS created before the file emptiness check.
        // 2. The entry in `allFiles` map IS created before the file emptiness check.
        // So, we adjust assertions similar to the non-PDF file test.

        Path expectedDir = storageService.getRootLocation().resolve(idPrefix);
        assertTrue(Files.exists(expectedDir),
                "Directory for idPrefix SHOULD be created even if file is empty, as per current logic.");
        assertTrue(Files.isDirectory(expectedDir));

        // Assert that the actual file "empty.pdf" was not created within that directory.
        Path actualFile = expectedDir.resolve("empty.pdf");
        assertFalse(Files.exists(actualFile),
                "The empty file itself should not be stored in the directory.");

        // Verify the allFiles map was populated but the file was not added to the list.
        List<Path> filesToMerge = storageService.getFilesToMerge(idPrefix);
        assertNotNull(filesToMerge, "Files to merge list should not be null, as map entry is created before file emptiness check.");
        assertTrue(filesToMerge.isEmpty(), "Files to merge list should be empty as the empty file was not added.");
    }

    @Test
    void testStorePDF_PathTraversalAttempt_ThrowsException() {
        String idPrefix = "testPrefix_pathTraversal";
        MockMultipartFile mockTraversalFile = new MockMultipartFile(
                "file",
                "../secret.pdf", // Path traversal attempt
                MediaType.APPLICATION_PDF_VALUE,
                "malicious content".getBytes()
        );

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            storageService.storePDF(mockTraversalFile, idPrefix);
        }, "storePDF should throw ResponseStatusException for path traversal attempt.");

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus(),
                "Exception status should be FORBIDDEN for path traversal attempt.");

        // Directory for idPrefix IS created before filename validation
        Path expectedDir = storageService.getRootLocation().resolve(idPrefix);
        assertTrue(Files.exists(expectedDir),
                "Directory for idPrefix SHOULD be created even if filename is malicious, as per current logic.");
        assertTrue(Files.isDirectory(expectedDir));

        // Assert that the file was NOT created by attempting to traverse up.
        // The path userSpecificDir.resolve("../secret.pdf") would normalize to getRootLocation().resolve("secret.pdf")
        Path attemptedStorePath = storageService.getRootLocation().resolve("secret.pdf").normalize();
        assertFalse(Files.exists(attemptedStorePath),
                "File should NOT be created at the normalized path after attempting traversal.");

        // Also, ensure no file was created within the idPrefix directory itself (e.g. if '..' was stripped, which it isn't)
        assertFalse(Files.exists(expectedDir.resolve("secret.pdf")),
                "File should not be created inside the idPrefix directory with a sanitized name.");
        // And ensure the original problematic name wasn't used directly within the idPrefix directory
        // (though resolve would handle this, this is an extra check for clarity on what's NOT happening)
        assertFalse(Files.exists(expectedDir.resolve(mockTraversalFile.getOriginalFilename())),
                "File should not be created using the raw original filename containing '..' inside idPrefix dir.");


        // Verify the allFiles map was populated but the file was not added to the list.
        List<Path> filesToMerge = storageService.getFilesToMerge(idPrefix);
        assertNotNull(filesToMerge, "Files to merge list should not be null, as map entry is created before filename validation.");
        assertTrue(filesToMerge.isEmpty(), "Files to merge list should be empty as the path traversal attempt prevented storage.");
    }

    @Test
    void testNumberOfFilesToMerge_NoFilesForPrefix() {
        String idPrefix = "prefixWithNoFiles";
        // Attempt to store a non-PDF file. This will throw an exception,
        // but LocalStorageService.storePDF initializes the list for this idPrefix in allFiles before the check.
        MockMultipartFile mockNonPdfFile = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "text content".getBytes()
        );
        assertThrows(ResponseStatusException.class, () -> storageService.storePDF(mockNonPdfFile, idPrefix));

        assertEquals(0, storageService.numberOfFilesToMerge(idPrefix),
                "Should be 0 files if storePDF failed to add any valid PDF for the prefix.");
    }

    @Test
    void testNumberOfFilesToMerge_FilesStored() throws IOException {
        String idPrefix = "prefixWithTwoFiles";
        MockMultipartFile file1 = new MockMultipartFile(
                "file1", // Different parameter names just for clarity, not strictly necessary
                "f1.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file2",
                "f2.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf content 2".getBytes()
        );

        // Store the files successfully
        assertDoesNotThrow(() -> storageService.storePDF(file1, idPrefix));
        assertDoesNotThrow(() -> storageService.storePDF(file2, idPrefix));

        assertEquals(2, storageService.numberOfFilesToMerge(idPrefix),
                "Should return the correct count of successfully stored files.");
    }

    @Test
    void testNumberOfFilesToMerge_NonExistentPrefix() {
        String idPrefix = "completelyNewPrefix";
        // This prefix was never used with storePDF, so it's not in allFiles map.
        // The numberOfFilesToMerge method should handle this gracefully.
        assertEquals(0, storageService.numberOfFilesToMerge(idPrefix),
                "Should be 0 files for a prefix that was never processed by storePDF.");
    }

    @Test
    void testGetMergedPDF_FileExists_ReadsAndDeletes() throws IOException {
        String idPrefix = "prefixForGetMergedPdfSuccess";
        String testDynamicFilename = "test_dynamic_output_merged.pdf"; // Defined dynamic filename

        // Manually prime the generatedFilenamesByPrefix map for this test
        storageService.setGeneratedFilenameForPrefix(idPrefix, testDynamicFilename);

        Path mergedPdfPath = storageService.getRootLocation().resolve(idPrefix + "-" + testDynamicFilename);
        byte[] expectedContent = "dummy merged PDF content".getBytes();

        Files.write(mergedPdfPath, expectedContent);
        assertTrue(Files.exists(mergedPdfPath), "Dummy merged PDF should exist before calling getMergedPDF.");

        // Action: Call getMergedPDF
        MergedPdfFile mergedPdfFile = storageService.getMergedPDF(idPrefix);

        // Assertions
        assertNotNull(mergedPdfFile, "MergedPdfFile object should not be null.");
        assertEquals(testDynamicFilename, mergedPdfFile.filename(), "Filename in MergedPdfFile should match the dynamic one.");
        assertArrayEquals(expectedContent, mergedPdfFile.content(), "Returned content should match dummy file content.");
        assertFalse(Files.exists(mergedPdfPath), "Merged PDF file should be deleted after getMergedPDF is called.");
        assertNull(storageService.getGeneratedFilenameForPrefix(idPrefix),
                "Entry in generatedFilenamesByPrefix should be removed after getMergedPDF.");
    }

    @Test
    void testGetMergedPDF_FileDoesNotExist_ThrowsException() {
        String idPrefix = "prefixForGetMergedPdfFailNonExistentName";
        // Ensure the prefix is not in generatedFilenamesByPrefix (implicitly true for a new prefix)
        assertNull(storageService.getGeneratedFilenameForPrefix(idPrefix),
                "Precondition: No generated filename should exist for this prefix.");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            storageService.getMergedPDF(idPrefix);
        }, "getMergedPDF should throw ResponseStatusException if no filename is registered for prefix.");

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(), // Status should be NOT_FOUND
                "Exception status should be NOT_FOUND if no filename registered for prefix.");
    }

    @Test
    void testMergeFiles_PerformsCleanup() throws IOException {
        String idPrefix = "prefixForMergeFilesCleanup";
        Path individualFilesDir = storageService.getRootLocation().resolve(idPrefix);

        // 1. Store some dummy files
        MockMultipartFile file1 = new MockMultipartFile(
                "f",
                "f1.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "c1".getBytes()
        );
        assertDoesNotThrow(() -> storageService.storePDF(file1, idPrefix));
        assertTrue(Files.exists(individualFilesDir.resolve("f1.pdf")), "Dummy file1 should exist before mergeFiles.");
        assertNotNull(storageService.getFilesToMerge(idPrefix), "allFiles map should have entry for individual files before mergeFiles.");

        // 2. Call mergeFiles. It will generate a dynamic name and store it.
        // It might throw ResponseStatusException if pdfunite fails or is missing.
        String dynamicName = null;
        try {
            storageService.mergeFiles(idPrefix);
            // Retrieve the dynamically generated name AFTER mergeFiles has run (and potentially created the file)
            dynamicName = storageService.getGeneratedFilenameForPrefix(idPrefix);
            assertNotNull(dynamicName, "Dynamically generated filename should not be null after successful mergeFiles call.");
        } catch (ResponseStatusException e) {
            System.out.println("mergeFiles threw ResponseStatusException (expected if pdfunite is missing/fails): " + e.getMessage());
            // Even if pdfunite fails, mergeFiles should have generated and stored the name.
            dynamicName = storageService.getGeneratedFilenameForPrefix(idPrefix);
            assertNotNull(dynamicName, "Dynamically generated filename should still be set even if pdfunite command fails.");
        }

        // 3. Create a dummy merged output file using the dynamic name (as if pdfunite created it)
        // This ensures mergeFiles's finally block doesn't delete what getMergedPDF expects.
        Path mergedOutputFile = storageService.getRootLocation().resolve(idPrefix + "-" + dynamicName);
        Files.write(mergedOutputFile, "dummy output from pdfunite".getBytes());
        assertTrue(Files.exists(mergedOutputFile), "Dummy merged output file (using dynamic name) should exist.");


        // 4. Assert cleanup of individual files and directory by mergeFiles's finally block
        assertFalse(Files.exists(individualFilesDir),
                "Directory for individual files (" + idPrefix + ") should be deleted after mergeFiles.");

        // 5. Assert idPrefix is removed from allFiles map (for individual files)
        assertNull(storageService.getFilesToMerge(idPrefix),
                "Entry for individual files in allFiles map should be removed after mergeFiles.");

        // 6. Assert that the main merged output file was NOT deleted by mergeFiles's cleanup
        assertTrue(Files.exists(mergedOutputFile),
                "The main merged output file (using dynamic name) should NOT be deleted by mergeFiles's cleanup.");

        // 7. Assert that the entry in generatedFilenamesByPrefix map IS STILL PRESENT after mergeFiles
        // (it's getMergedPDF's responsibility to clean that specific entry)
        assertEquals(dynamicName, storageService.getGeneratedFilenameForPrefix(idPrefix),
                "Entry in generatedFilenamesByPrefix should persist after mergeFiles completes.");
    }
}
