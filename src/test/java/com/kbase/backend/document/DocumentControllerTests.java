package com.kbase.backend.document;

import com.kbase.backend.document.dto.CreateDocumentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentControllerTests {

    @Test
    void createUsesAuthenticatedPrincipalWithoutAcceptingActingUserId() {
        DocumentService service = mock(DocumentService.class);
        UserDetails principal = mock(UserDetails.class);
        UUID projectId = UUID.randomUUID();
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Guide",
                null,
                "guide.pdf",
                "projects/key.pdf",
                "application/pdf",
                100
        );
        when(principal.getUsername()).thenReturn("member@example.com");

        new DocumentController(service).create(projectId, request, principal);

        verify(service).create(projectId, request, "member@example.com");
    }
}
