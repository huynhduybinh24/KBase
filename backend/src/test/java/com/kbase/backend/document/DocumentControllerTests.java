package com.kbase.backend.document;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentControllerTests {

    @Test
    void createUsesAuthenticatedPrincipalWithoutActingUserId() {
        DocumentService service = mock(DocumentService.class);
        UserDetails principal = mock(UserDetails.class);
        UUID projectId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "guide.pdf", "application/pdf", new byte[]{1});
        when(principal.getUsername()).thenReturn("member@example.com");

        new DocumentController(service).create(projectId, file, "Guide", null, principal);

        verify(service).create(projectId, file, "Guide", null, "member@example.com");
    }

    @Test
    void downloadAndPreviewSetExpectedDisposition() {
        DocumentService service = mock(DocumentService.class);
        UserDetails principal = mock(UserDetails.class);
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(principal.getUsername()).thenReturn("member@example.com");
        when(service.download(projectId, documentId, "member@example.com"))
                .thenReturn(content());
        when(service.preview(projectId, documentId, "member@example.com"))
                .thenReturn(content());
        DocumentController controller = new DocumentController(service);

        var download = controller.download(projectId, documentId, principal);
        var preview = controller.preview(projectId, documentId, principal);

        assertTrue(download.getHeaders().getContentDisposition().isAttachment());
        assertTrue(preview.getHeaders().getContentDisposition().isInline());
        assertEquals(3, download.getHeaders().getContentLength());
    }

    private DocumentService.DocumentContent content() {
        return new DocumentService.DocumentContent("guide.pdf", "application/pdf", 3,
                new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }
}
