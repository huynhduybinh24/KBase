package com.kbase.backend.document.extraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class PdfTextExtractor implements TextExtractor {

    @Override
    public boolean supports(String contentType) {
        return "application/pdf".equals(contentType);
    }

    @Override
    public String extract(InputStream input) throws Exception {
        try (var document = Loader.loadPDF(input.readAllBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }
}
