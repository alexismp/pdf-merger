package org.alexismp.pdfmerger;

import org.alexismp.pdfmerger.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus; // Added import
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException; // Added import

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// No need for MockMvcBuilders.standaloneSetup when using @SpringBootTest and @AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
public class PDFMergerControllerTests {

    @Autowired
    private MockMvc mvc; // Changed from mockMvc to mvc to match existing variable name

    @MockBean
    private StorageService storageService;

    @Test
    public void testHandleFileUpload_NoFiles() throws Exception {
        // When no files are uploaded, numberOfFilesToMerge should be 0
        // The controller generates a prefix and calls numberOfFilesToMerge with it.
        when(storageService.numberOfFilesToMerge(anyString())).thenReturn(0);

        mvc.perform(multipart("/pdfmerger")) // Using existing 'mvc' field
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        // Verify that storageService.numberOfFilesToMerge was called once
        verify(storageService, times(1)).numberOfFilesToMerge(anyString());
        // Verify other storageService methods were not called
        verify(storageService, never()).storePDF(any(), anyString());
        verify(storageService, never()).mergeFiles(anyString());
        verify(storageService, never()).getMergedPDF(anyString());
    }
    // Test methods will be added here in future steps

    @Test
    public void testHandleFileUpload_OneFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", // Name of the request parameter
                "file1.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf_content_for_file1".getBytes()
        );

        byte[] mergedPdfContent = "merged_pdf_content".getBytes();

        // Mocking StorageService behavior
        // storePDF is void, so doNothing is appropriate if we just want to ensure it's called
        doNothing().when(storageService).storePDF(any(MultipartFile.class), anyString());
        when(storageService.numberOfFilesToMerge(anyString())).thenReturn(1);
        doNothing().when(storageService).mergeFiles(anyString());
        when(storageService.getMergedPDF(anyString())).thenReturn(mergedPdfContent);

        mvc.perform(multipart("/pdfmerger").file(file))
                .andExpect(status().isOk())
                .andExpect(content().bytes(mergedPdfContent));

        // Verify interactions with storageService
        // The controller generates a UUID prefix, so we use anyString() for that argument.
        verify(storageService, times(1)).storePDF(eq(file), anyString());
        verify(storageService, times(1)).numberOfFilesToMerge(anyString());
        verify(storageService, times(1)).mergeFiles(anyString());
        verify(storageService, times(1)).getMergedPDF(anyString());
    }

    @Test
    public void testHandleFileUpload_MultipleFiles() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "file1.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf_content_file1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "file2.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf_content_file2".getBytes()
        );

        byte[] mergedPdfContent = "merged_pdf_content_multiple".getBytes();

        // Mocking StorageService behavior
        doNothing().when(storageService).storePDF(any(MultipartFile.class), anyString());
        when(storageService.numberOfFilesToMerge(anyString())).thenReturn(2); // For two files
        doNothing().when(storageService).mergeFiles(anyString());
        when(storageService.getMergedPDF(anyString())).thenReturn(mergedPdfContent);

        mvc.perform(multipart("/pdfmerger").file(file1).file(file2))
                .andExpect(status().isOk())
                .andExpect(content().bytes(mergedPdfContent));

        // Verify interactions with storageService
        // The controller generates a UUID prefix, so we use anyString() for that argument.
        // storePDF is called for each file with the same prefix.
        verify(storageService, times(1)).storePDF(eq(file1), anyString());
        verify(storageService, times(1)).storePDF(eq(file2), anyString());
        verify(storageService, times(1)).numberOfFilesToMerge(anyString()); // Called once after all files are stored
        verify(storageService, times(1)).mergeFiles(anyString());
        verify(storageService, times(1)).getMergedPDF(anyString());
    }

    @Test
    public void testHandleFileUpload_NonPdfFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "file1.txt", // Non-PDF extension
                MediaType.TEXT_PLAIN_VALUE, // Appropriate content type
                "some_text_content".getBytes()
        );

        // Mocking StorageService behavior
        // Configure storePDF to throw ResponseStatusException for non-PDFs
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are allowed"))
            .when(storageService).storePDF(eq(file), anyString());

        mvc.perform(multipart("/pdfmerger").file(file))
                .andExpect(status().isBadRequest()); // Expecting a 400 Bad Request

        // Verify interactions with storageService
        verify(storageService, times(1)).storePDF(eq(file), anyString());
        verify(storageService, never()).numberOfFilesToMerge(anyString());
        verify(storageService, never()).mergeFiles(anyString());
        verify(storageService, never()).getMergedPDF(anyString());
    }

    @Test
    public void testHandleFileUpload_EmptyOriginalFilename() throws Exception {
        MockMultipartFile fileWithEmptyName = new MockMultipartFile(
                "files", // Name of the request parameter
                "",      // Empty original filename
                MediaType.APPLICATION_PDF_VALUE,
                "pdf_content".getBytes() // Content can be non-empty for this test
        );

        // Mocking StorageService behavior
        // storePDF should not be called.
        // numberOfFilesToMerge will be called, and since storePDF wasn't, it should reflect 0 files to merge.
        when(storageService.numberOfFilesToMerge(anyString())).thenReturn(0);

        mvc.perform(multipart("/pdfmerger").file(fileWithEmptyName))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        // Verify interactions with storageService
        verify(storageService, never()).storePDF(any(MultipartFile.class), anyString());
        verify(storageService, times(1)).numberOfFilesToMerge(anyString());
        verify(storageService, never()).mergeFiles(anyString());
        verify(storageService, never()).getMergedPDF(anyString());
    }

    @Test
    public void testHandleFileUpload_EmptyFileContent() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "files",
                "empty_file.pdf", // Non-empty original filename
                MediaType.APPLICATION_PDF_VALUE,
                new byte[0]       // Empty content
        );

        // Mocking StorageService behavior
        // storePDF will be called, and it should throw an exception for an empty file.
        doThrow(new ResponseStatusException(HttpStatus.NO_CONTENT, "File is empty"))
            .when(storageService).storePDF(eq(emptyFile), anyString());

        mvc.perform(multipart("/pdfmerger").file(emptyFile))
                .andExpect(status().isNoContent()); // Expecting a 204 No Content

        // Verify interactions with storageService
        verify(storageService, times(1)).storePDF(eq(emptyFile), anyString());
        verify(storageService, never()).numberOfFilesToMerge(anyString());
        verify(storageService, never()).mergeFiles(anyString());
        verify(storageService, never()).getMergedPDF(anyString());
    }
}
