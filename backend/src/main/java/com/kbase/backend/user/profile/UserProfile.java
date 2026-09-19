package com.kbase.backend.user.profile;

import com.kbase.backend.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "avatar_type", nullable = false, length = 20)
    private AvatarType avatarType = AvatarType.NONE;

    @Column(name = "avatar_preset", length = 40)
    private String avatarPreset;

    @Column(name = "avatar_object_key", length = 1000)
    private String avatarObjectKey;

    @Column(name = "avatar_content_type", length = 100)
    private String avatarContentType;

    @Column(name = "avatar_size")
    private Long avatarSize;

    @Column(name = "avatar_updated_at")
    private Instant avatarUpdatedAt;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {}

    public UserProfile(User user, String fullName) {
        this.user = user;
        this.fullName = fullName;
        this.displayName = fullName;
    }

    public void updateDisplayName(String displayName) { this.displayName = displayName; }
    public void usePreset(String preset) { avatarType = AvatarType.PRESET; avatarPreset = preset; clearUpload(); avatarUpdatedAt = Instant.now(); }
    public void useUpload(String key, String contentType, long size) { avatarType = AvatarType.UPLOAD; avatarPreset = null; avatarObjectKey = key; avatarContentType = contentType; avatarSize = size; avatarUpdatedAt = Instant.now(); }
    public void clearAvatar() { avatarType = AvatarType.NONE; avatarPreset = null; clearUpload(); avatarUpdatedAt = Instant.now(); }
    private void clearUpload() { avatarObjectKey = null; avatarContentType = null; avatarSize = null; }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getFullName() { return fullName; }
    public String getDisplayName() { return displayName; }
    public AvatarType getAvatarType() { return avatarType; }
    public String getAvatarPreset() { return avatarPreset; }
    public String getAvatarObjectKey() { return avatarObjectKey; }
    public String getAvatarContentType() { return avatarContentType; }
    public Long getAvatarSize() { return avatarSize; }
    public Instant getAvatarUpdatedAt() { return avatarUpdatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
