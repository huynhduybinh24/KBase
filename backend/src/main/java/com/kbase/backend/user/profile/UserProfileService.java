package com.kbase.backend.user.profile;

import com.kbase.backend.config.ProfileProperties;
import com.kbase.backend.exception.*;
import com.kbase.backend.storage.StorageService;
import com.kbase.backend.user.User;
import com.kbase.backend.user.UserRepository;
import com.kbase.backend.user.profile.dto.UpdateUserProfileRequest;
import com.kbase.backend.user.profile.dto.UserProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserProfileService {
    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private static final Set<String> PRESETS = Set.of("violet", "blue", "rose", "teal");

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final StorageService storage;
    private final ProfileProperties properties;
    private final TransactionTemplate transactions;

    public UserProfileService(UserRepository users, UserProfileRepository profiles,
                              StorageService storage, ProfileProperties properties,
                              TransactionTemplate transactions) {
        this.users = users;
        this.profiles = profiles;
        this.storage = storage;
        this.properties = properties;
        this.transactions = transactions;
    }

    public void createForRegistration(User user, String suppliedFullName) {
        String fullName = normalizeOptionalName(suppliedFullName);
        profiles.save(new UserProfile(user, fullName));
    }

    public UserProfileResponse getCurrentProfile(String email) {
        User user = requireUser(email);
        return UserProfileResponse.from(user, profiles.findByUserId(user.getId()).orElse(null));
    }

    public UserProfileResponse updateCurrentProfile(String email, UpdateUserProfileRequest request) {
        User user = requireUser(email);
        String displayName = normalizeRequiredName(request.displayName());
        String[] oldUploadKey = new String[1];
        UserProfileResponse response = transactions.execute(status -> {
            UserProfile profile = profiles.findByUserId(user.getId()).orElseGet(() -> new UserProfile(user, null));
            profile.updateDisplayName(displayName);
            if (request.avatarType() == AvatarType.PRESET) {
                if (!PRESETS.contains(request.avatarPreset())) throw new BadRequestException("Invalid avatar preset");
                if (profile.getAvatarType() == AvatarType.UPLOAD) oldUploadKey[0] = profile.getAvatarObjectKey();
                profile.usePreset(request.avatarPreset());
            } else if (request.avatarType() == AvatarType.NONE) {
                if (profile.getAvatarType() == AvatarType.UPLOAD) oldUploadKey[0] = profile.getAvatarObjectKey();
                profile.clearAvatar();
            } else if (request.avatarType() == AvatarType.UPLOAD) {
                throw new BadRequestException("Use the avatar upload endpoint for uploaded avatars");
            }
            return UserProfileResponse.from(user, profiles.saveAndFlush(profile));
        });
        deleteOldQuietly(oldUploadKey[0]);
        log.info("Profile updated userId={}", user.getId());
        return response;
    }

    public UserProfileResponse uploadAvatar(String email, MultipartFile file) {
        User user = requireUser(email);
        if (file.isEmpty()) throw new BadRequestException("Avatar file is required");
        if (file.getSize() > properties.avatarMaxFileSize()) throw new FileTooLargeException("Avatar exceeds the configured maximum size");
        byte[] bytes;
        try { bytes = file.getBytes(); } catch (IOException exception) { throw new BadRequestException("Avatar could not be read"); }
        String contentType = detectImageType(bytes);
        String extension = switch (contentType) { case "image/png" -> "png"; case "image/jpeg" -> "jpg"; default -> "webp"; };
        String newKey = "users/%s/avatar/%s.%s".formatted(user.getId(), UUID.randomUUID(), extension);
        storage.upload(newKey, new ByteArrayInputStream(bytes), bytes.length, contentType);

        String[] oldKey = new String[1];
        UserProfileResponse response;
        try {
            response = transactions.execute(status -> {
                UserProfile profile = profiles.findByUserId(user.getId()).orElseGet(() -> new UserProfile(user, null));
                if (profile.getAvatarType() == AvatarType.UPLOAD) oldKey[0] = profile.getAvatarObjectKey();
                profile.useUpload(newKey, contentType, bytes.length);
                return UserProfileResponse.from(user, profiles.saveAndFlush(profile));
            });
        } catch (RuntimeException exception) {
            try { storage.delete(newKey); } catch (RuntimeException cleanupFailure) { log.error("Failed compensating avatar object cleanup userId={}", user.getId()); }
            throw exception;
        }
        deleteOldQuietly(oldKey[0]);
        log.info("Avatar uploaded userId={} contentType={} size={}", user.getId(), contentType, bytes.length);
        return response;
    }

    public AvatarDownload downloadAvatar(String email) {
        User user = requireUser(email);
        UserProfile profile = profiles.findByUserId(user.getId())
                .filter(value -> value.getAvatarType() == AvatarType.UPLOAD)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar not found"));
        return new AvatarDownload(storage.download(profile.getAvatarObjectKey()),
                profile.getAvatarContentType(), profile.getAvatarSize());
    }

    public void deleteAvatar(String email) {
        User user = requireUser(email);
        String[] oldKey = new String[1];
        transactions.execute(status -> {
            profiles.findByUserId(user.getId()).ifPresent(profile -> {
                if (profile.getAvatarType() == AvatarType.UPLOAD) oldKey[0] = profile.getAvatarObjectKey();
                profile.clearAvatar();
                profiles.saveAndFlush(profile);
            });
            return null;
        });
        deleteOldQuietly(oldKey[0]);
        log.info("Avatar reset userId={}", user.getId());
    }

    private User requireUser(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(InvalidCredentialsException::new);
    }

    private String normalizeRequiredName(String value) {
        if (value == null || value.trim().isEmpty()) throw new BadRequestException("Display name is required");
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 120) throw new BadRequestException("Display name must be 120 characters or fewer");
        return normalized;
    }

    private String normalizeOptionalName(String value) {
        if (value == null) return null;
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new BadRequestException("Full name must not be blank");
        if (normalized.length() > 120) throw new BadRequestException("Full name must be 120 characters or fewer");
        return normalized;
    }

    private String detectImageType(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        throw new UnsupportedMediaTypeException("Avatar must be a PNG, JPEG, or WebP image");
    }

    private void deleteOldQuietly(String key) {
        if (key == null) return;
        try { storage.delete(key); } catch (RuntimeException exception) { log.warn("Could not remove replaced avatar object keyHash={}", Integer.toHexString(key.hashCode())); }
    }
}
