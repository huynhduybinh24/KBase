package com.kbase.backend.user.profile;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileControllerTests {
    @Test
    void allActionsUseOnlyTheAuthenticatedPrincipal() {
        UserProfileService service = mock(UserProfileService.class);
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn("person@example.com");
        UserProfileController controller = new UserProfileController(service);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        controller.getProfile(principal);
        controller.uploadAvatar(principal, file);
        controller.deleteAvatar(principal);

        verify(service).getCurrentProfile("person@example.com");
        verify(service).uploadAvatar("person@example.com", file);
        verify(service).deleteAvatar("person@example.com");
    }

    @Test
    void avatarDownloadIsInlineWithPersistedMediaTypeAndBytes() throws Exception {
        UserProfileService service = mock(UserProfileService.class);
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn("person@example.com");
        byte[] bytes = {1, 2, 3};
        when(service.downloadAvatar("person@example.com"))
                .thenReturn(new AvatarDownload(new ByteArrayInputStream(bytes), "image/png", bytes.length));

        ResponseEntity<InputStreamResource> response = new UserProfileController(service).getAvatar(principal);
        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertEquals("inline", response.getHeaders().getFirst("Content-Disposition"));
        assertArrayEquals(bytes, response.getBody().getInputStream().readAllBytes());
    }
}
