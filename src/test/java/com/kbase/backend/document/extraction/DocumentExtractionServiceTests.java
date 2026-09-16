package com.kbase.backend.document.extraction;

import com.kbase.backend.document.Document;
import com.kbase.backend.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentExtractionServiceTests {

    private DocumentContentRepository repository;
    private StorageService storage;
    private Document document;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentContentRepository.class);
        storage = mock(StorageService.class);
        document = mock(Document.class);
        when(document.getId()).thenReturn(UUID.randomUUID());
        when(document.getStorageKey()).thenReturn("projects/key.txt");
        when(repository.saveAndFlush(any(DocumentContent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void successfulExtractionCompletesWithExactText() {
        when(document.getContentType()).thenReturn("text/plain");
        when(storage.download("projects/key.txt")).thenReturn(new ByteArrayInputStream(
                "exact content".getBytes(StandardCharsets.UTF_8)));
        var service = service(List.of(new PlainTextExtractor()));

        service.initializeAndExtract(document);

        var content = captureSavedContent();
        assertEquals(ExtractionStatus.COMPLETED, content.getExtractionStatus());
        assertEquals("exact content", content.getExtractedText());
    }

    @Test
    void unsupportedTypeIsPersistedWithoutReadingStorage() {
        when(document.getContentType()).thenReturn("image/png");
        var service = service(List.of(new PlainTextExtractor()));

        service.initializeAndExtract(document);

        assertEquals(ExtractionStatus.UNSUPPORTED, captureSavedContent().getExtractionStatus());
        verify(storage, never()).download(any());
    }

    @Test
    void extractionFailureIsSafeAndDoesNotDeleteObject() throws Exception {
        when(document.getContentType()).thenReturn("text/plain");
        when(storage.download("projects/key.txt")).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        TextExtractor failing = mock(TextExtractor.class);
        when(failing.supports("text/plain")).thenReturn(true);
        when(failing.extract(any())).thenThrow(new IllegalStateException("secret internals"));
        var service = service(List.of(failing));

        service.initializeAndExtract(document);

        DocumentContent content = captureSavedContent();
        assertEquals(ExtractionStatus.FAILED, content.getExtractionStatus());
        assertEquals("Text extraction failed", content.getExtractionError());
        verify(storage, never()).delete(any());
    }

    @Test
    void getReturnsPersistedState() {
        DocumentContent content = new DocumentContent(document);
        content.markUnsupported();
        when(repository.findByDocumentId(document.getId())).thenReturn(Optional.of(content));

        var response = service(List.of()).get(document.getId());

        assertEquals(ExtractionStatus.UNSUPPORTED, response.status());
    }

    private DefaultDocumentExtractionService service(List<TextExtractor> extractors) {
        return new DefaultDocumentExtractionService(repository, storage, extractors);
    }

    private DocumentContent captureSavedContent() {
        var captor = org.mockito.ArgumentCaptor.forClass(DocumentContent.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
