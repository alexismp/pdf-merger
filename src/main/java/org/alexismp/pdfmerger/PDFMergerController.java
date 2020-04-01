
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
	public @ResponseBody() byte[] handleFileUpload(@RequestParam("files") final MultipartFile[] files) {
		for (MultipartFile file : files) {
			if (!file.getOriginalFilename().isEmpty()) {
				storageService.storePDF(file);
			}
		}
		if (storageService.numberOfFilesToMerge() != 0) {
			storageService.mergeFiles();
			return storageService.getMergedPDF();
		} else {
			return null;
		}
	}

}