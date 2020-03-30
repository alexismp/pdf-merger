package org.alexismp.pdfmerger;

import java.util.StringTokenizer;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class FileOrderController {
	private final OrderService orderService;

	@Autowired
	public FileOrderController(final OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/order")
	public String handleFileOrder(@RequestBody final String orderedFiles) {
		orderService.clear();
		final StringTokenizer sb = new StringTokenizer(orderedFiles, "< >", false);
		while (sb.hasMoreTokens()) {
			final String fileName = sb.nextToken();
			if (fileName.length() < 4 ) continue;	// <p> or 1. ... 12. (will break if more than 100 files selected)
			orderService.addFile(fileName);
		}
        System.out.println(orderService.listAllFilesInOrder());
		return "redirect:/";
	}

}