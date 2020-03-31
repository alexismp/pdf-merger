package org.alexismp.pdfmerger;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class PDFMergerController {

	private final StorageService storageService;

	@Autowired
	public PDFMergerController(final StorageService storageService) {
		this.storageService = storageService;
	}

	@PostMapping(value = "/pdfmerger", produces = MediaType.APPLICATION_PDF_VALUE)
	public @ResponseBody() byte[] handleFileUpload(@RequestParam("files") final MultipartFile[] files) throws IOException {
		for (MultipartFile file : files) {
			if (!file.getOriginalFilename().isEmpty()) {
				storageService.storePDF(file);
			}
		}
		storageService.mergeFiles();
		return storageService.getMergedPDF();
	}

}