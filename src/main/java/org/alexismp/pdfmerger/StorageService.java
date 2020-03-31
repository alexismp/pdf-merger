package org.alexismp.pdfmerger;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
	void init();
	void storePDF(MultipartFile file);
	void deleteTmpFiles();
	void mergeFiles();
	byte[] getMergedPDF() throws IOException;
}