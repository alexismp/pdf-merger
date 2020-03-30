package org.alexismp.pdfmerger;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class SimpleOrderService implements OrderService {
    private List<String> filesToMerge;

    @Autowired
    public SimpleOrderService() {
        this.filesToMerge = Collections.synchronizedList(new ArrayList<String>());
    }

    @Override
    public void init() {
        filesToMerge.clear();
    }

    @Override
    public void addFile(String file) {
        filesToMerge.add(file);
    }

    @Override
    public void clear() {
        filesToMerge.clear();
    }

    @Override
    public List<String> listAllFilesInOrder() {
        return filesToMerge;
    }

    @Override
    public int size() {
        return filesToMerge.size();
    }
    
}