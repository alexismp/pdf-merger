package org.alexismp.pdfmerger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileUploadController {

	private final StorageService storageService;
	private final OrderService orderService;

	@Autowired
	public FileUploadController(final StorageService storageService, final OrderService orderService) {
		this.storageService = storageService;
		this.orderService = orderService;
	}

	@PostMapping("/uploader")
	public String handleFileUpload(@RequestParam("file") final MultipartFile file,
			final RedirectAttributes redirectAttributes) {
		storageService.store(file);
		System.out.println("Successfully saved " + file.getOriginalFilename()
				+ " (" + storageService.numFilesSaved()
				+ " of " + orderService.size() + ")");
		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded " + file.getOriginalFilename() + "!");

		// redirect to actual merge controller
		// track number of uploads, when all done redirect to /merger
		if (storageService.numFilesSaved() >= orderService.size()) {
			System.out.println("All files uploaded, sending to merge.");
			storageService.mergeFiles(orderService.listAllFilesInOrder());
			return "redirect:/result";
		}
		return "redirect:";
	}

	@GetMapping(value = "/result")
	public @ResponseBody byte[] getImage() throws IOException {
		return storageService.getMergedPDF();
	}
}