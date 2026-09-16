package com.kbase.backend.document.extraction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextExtractorTests {

    @Test
    void plainTextExtractionIsExactAndMarkdownIsSupported() throws Exception {
        PlainTextExtractor extractor = new PlainTextExtractor();
        String expected = "KBase exact text\nSecond line";

        assertTrue(extractor.supports("text/plain"));
        assertTrue(extractor.supports("text/markdown"));
        assertEquals(expected, extractor.extract(new ByteArrayInputStream(
                expected.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void pdfExtractionReturnsRecognizableText() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 700);
                content.showText("KBase searchable PDF text");
                content.endText();
            }
            document.save(output);
            pdf = output.toByteArray();
        }

        String extracted = new PdfTextExtractor().extract(new ByteArrayInputStream(pdf));

        assertTrue(extracted.contains("KBase searchable PDF text"));
    }
}
