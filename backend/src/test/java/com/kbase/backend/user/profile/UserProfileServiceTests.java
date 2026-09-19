package com.kbase.backend.user.profile;

import com.kbase.backend.config.ProfileProperties;
import com.kbase.backend.exception.FileTooLargeException;
import com.kbase.backend.exception.BadRequestException;
import com.kbase.backend.exception.ResourceNotFoundException;
import com.kbase.backend.exception.UnsupportedMediaTypeException;
import com.kbase.backend.storage.StorageService;
import com.kbase.backend.user.Role;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import com.kbase.backend.user.profile.dto.UpdateUserProfileRequest;
import com.kbase.backend.user.profile.dto.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTests {
    @Mock UserRepository users;
    @Mock UserProfileRepository profiles;
    @Mock StorageService storage;
    @Mock TransactionTemplate transactions;
    private User user;
    private UserProfileService service;

    @BeforeEach
    void setUp() {
        user = new User("person@example.com", "hash", Set.of(Role.USER));
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        service = new UserProfileService(users, profiles, storage,
                new ProfileProperties(2 * 1024 * 1024), transactions);
        lenient().when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        lenient().when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        lenient().when(profiles.saveAndFlush(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void existingUserWithoutRowGetsSafeFallback() {
        when(profiles.findByUserId(user.getId())).thenReturn(Optional.empty());
        UserProfileResponse response = service.getCurrentProfile("person@example.com");
        assertEquals("person", response.displayName());
        assertEquals(AvatarType.NONE, response.avatarType());
        assertFalse(response.hasAvatar());
    }

    @Test
    void registrationCreatesProfileWithFullAndDisplayName() {
        when(profiles.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
        service.createForRegistration(user, "  Person   Example  ");
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profiles).save(captor.capture());
        assertEquals("Person Example", captor.getValue().getFullName());
        assertEquals("Person Example", captor.getValue().getDisplayName());
    }

    @Test
    void updatePersistsTrimmedDisplayNameAndAllowedPreset() {
        UserProfile profile = new UserProfile(user, "Person Example");
        when(profiles.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        UserProfileResponse response = service.updateCurrentProfile("person@example.com",
                new UpdateUserProfileRequest("  Duy   Binh  ", AvatarType.PRESET, "blue"));
        assertEquals("Duy Binh", response.displayName());
        assertEquals(AvatarType.PRESET, response.avatarType());
        assertEquals("blue", response.avatarPreset());
    }

    @Test
    void rejectsBlankDisplayNameAndUnknownPreset() {
        UserProfile profile = new UserProfile(user, null);
        when(profiles.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        assertThrows(BadRequestException.class, () -> service.updateCurrentProfile("person@example.com",
                new UpdateUserProfileRequest("   ", null, null)));
        assertThrows(BadRequestException.class, () -> service.updateCurrentProfile("person@example.com",
                new UpdateUserProfileRequest("Person", AvatarType.PRESET, "external-url")));
    }

    @Test
    void rejectsOversizedAndUnsupportedAvatar() {
        assertThrows(FileTooLargeException.class, () -> service.uploadAvatar("person@example.com",
                new MockMultipartFile("file", "large.png", "image/png", new byte[2 * 1024 * 1024 + 1])));
        assertThrows(UnsupportedMediaTypeException.class, () -> service.uploadAvatar("person@example.com",
                new MockMultipartFile("file", "fake.png", "image/png", "not-image".getBytes())));
        verifyNoInteractions(storage);
    }

    @Test
    void pngJpegAndWebpAreDetectedFromBytesAndUploaded() {
        when(profiles.findByUserId(user.getId())).thenReturn(Optional.empty());
        byte[][] files = {
                {(byte)0x89,'P','N','G',0x0d,0x0a,0x1a,0x0a},
                {(byte)0xff,(byte)0xd8,(byte)0xff,0x00},
                {'R','I','F','F',0,0,0,0,'W','E','B','P'}
        };
        String[] types = {"image/png", "image/jpeg", "image/webp"};
        for (int i = 0; i < files.length; i++) {
            service.uploadAvatar("person@example.com", new MockMultipartFile("file", "avatar", types[i], files[i]));
        }
        for (String type : types) verify(storage).upload(anyString(), any(), anyLong(), eq(type));
    }

    @Test
    void replacementAssociatesNewObjectBeforeDeletingOldObject() {
        UserProfile profile = new UserProfile(user, null);
        profile.useUpload("users/old.png", "image/png", 8);
        when(profiles.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        service.uploadAvatar("person@example.com", new MockMultipartFile("file", "new.png", "image/png",
                new byte[]{(byte)0x89,'P','N','G',0x0d,0x0a,0x1a,0x0a}));
        verify(profiles).saveAndFlush(profile);
        verify(storage).delete("users/old.png");
        assertNotEquals("users/old.png", profile.getAvatarObjectKey());
    }

    @Test
    void databaseFailureCompensatesNewUpload() {
        when(profiles.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(profiles.saveAndFlush(any())).thenThrow(new IllegalStateException("db failed"));
        assertThrows(IllegalStateException.class, () -> service.uploadAvatar("person@example.com",
                new MockMultipartFile("file", "new.png", "image/png",
                        new byte[]{(byte)0x89,'P','N','G',0x0d,0x0a,0x1a,0x0a})));
        verify(storage).delete(startsWith("users/"));
    }

    @Test
    void downloadReturnsExactStoredContentAndDeleteResetsMetadata() throws Exception {
        byte[] bytes = {(byte)0x89,'P','N','G',0x0d,0x0a,0x1a,0x0a};
        UserProfile profile = new UserProfile(user, null);
        profile.useUpload("users/avatar.png", "image/png", bytes.length);
        when(profiles.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(storage.download("users/avatar.png")).thenReturn(new ByteArrayInputStream(bytes));
        assertArrayEquals(bytes, service.downloadAvatar("person@example.com").content().readAllBytes());
        service.deleteAvatar("person@example.com");
        assertEquals(AvatarType.NONE, profile.getAvatarType());
        verify(storage).delete("users/avatar.png");
        assertThrows(ResourceNotFoundException.class, () -> service.downloadAvatar("person@example.com"));
    }
}
