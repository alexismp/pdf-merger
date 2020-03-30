package org.alexismp.pdfmerger;

import java.util.List;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
		System.out.println("Successfully saved " + file.getOriginalFilename() + " (" + storageService.numFilesSaved()
				+ " of " + orderService.size() + ")");
		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded " + file.getOriginalFilename() + "!");

		// redirect to actual merge controller
		// track number of uploads, when all done redirect to /merger
		if (storageService.numFilesSaved() >= orderService.size()) {
			System.out.println("All files uploaded, sending to merge.");
			mergeFiles();
		}
		return "redirect:";
	}

	public void mergeFiles() {
		final StringBuilder mergeCommand = new StringBuilder(
				"/usr/bin/gs -sDEVICE=pdfwrite -dCompatibilityLevel=1.4 -dPDFSETTINGS=/default -dNOPAUSE -dQUIET -dBATCH -dDetectDuplicateImages -dCompressFonts=true -r150 -sOutputFile=output.pdf ");

		// add all files in the order they're specified
		final List<String> filenames = orderService.listAllFilesInOrder();
		for (final String filename : filenames) {
			mergeCommand.append(storageService.getRootLocation().toString() + "/" + filename);
			mergeCommand.append(" ");
		}
		final ProcessBuilder builder = new ProcessBuilder();
		builder.command("sh", "-c", mergeCommand.toString());

		try {
			final Process process = builder.inheritIO().start();
			final int exitCode = process.waitFor();
			System.out.println("\nExited with error code : " + exitCode);
		} catch (IOException | InterruptedException e) {
			System.err.println(e);
		}

		// storageService.deleteAll();
		// return "redirect:/";
	}

}