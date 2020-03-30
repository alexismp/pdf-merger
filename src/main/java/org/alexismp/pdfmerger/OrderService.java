package org.alexismp.pdfmerger;

import java.util.List;

public interface OrderService {
    void init();
    void clear();
    void addFile(String file);
    List<String> listAllFilesInOrder();
    int size();
}