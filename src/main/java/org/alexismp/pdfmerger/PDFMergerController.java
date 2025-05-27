
/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alexismp.pdfmerger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
// Removed @ResponseBody as ResponseEntity is used
import org.springframework.web.multipart.MultipartFile;

// Added imports
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Controller
public class PDFMergerController {

	private final StorageService storageService;

	@Autowired
	public PDFMergerController(final StorageService storageService) {
		this.storageService = storageService;
	}

	@PostMapping(value = "/pdfmerger") // Removed produces and @ResponseBody
	public ResponseEntity<byte[]> handleFileUpload(@RequestParam("files") final MultipartFile[] files) {
		UUID prefix = UUID.randomUUID();

		for (MultipartFile file : files) {
			// Added null check for getOriginalFilename() for robustness
			if (file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) { // ignore empty form inputs
				storageService.storePDF(file, prefix.toString());
			}
		}

		if (storageService.numberOfFilesToMerge(prefix.toString()) != 0) {
			storageService.mergeFiles(prefix.toString());
			MergedPdfFile mergedPdfFile = storageService.getMergedPDF(prefix.toString()); // Get the object

			if (mergedPdfFile == null || mergedPdfFile.content() == null) {
				// This case should ideally not happen if getMergedPDF throws exceptions for errors
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			// Ensure filename is properly quoted.
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="" + mergedPdfFile.filename() + """);

			return new ResponseEntity<>(mergedPdfFile.content(), headers, HttpStatus.OK);
		} else { // no files to merge
			return ResponseEntity.noContent().build();
		}
	}

}