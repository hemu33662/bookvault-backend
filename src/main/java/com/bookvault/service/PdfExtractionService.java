package com.bookvault.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfExtractionService {

    public List<Map<String, Object>> extractContentToJson(MultipartFile file) throws IOException {
        List<Map<String, Object>> pages = new ArrayList<>();

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);

                Map<String, Object> pageData = new HashMap<>();
                pageData.put("pageNumber", i);
                pageData.put("text", text.trim());
                pageData.put("title", "Page " + i);
                
                // You can add more logic here to extract images or titles
                pages.add(pageData);
            }
        }

        return pages;
    }
}
