package com.kbase.backend.auth.google;

import com.kbase.backend.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_external_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_external_identity_subject", columnNames = {"provider", "provider_subject"}),
        @UniqueConstraint(name = "uq_external_identity_user_provider", columnNames = {"user_id", "provider"})
})
public class UserExternalIdentity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ExternalIdentityProvider provider;
    @Column(name = "provider_subject", nullable = false, length = 255) private String providerSubject;
    @Column(name = "provider_email", length = 320) private String providerEmail;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected UserExternalIdentity() {}
    public UserExternalIdentity(User user, ExternalIdentityProvider provider, String subject, String email) {
        this.user = user; this.provider = provider; this.providerSubject = subject; this.providerEmail = email;
    }
    public User getUser() { return user; }
}
