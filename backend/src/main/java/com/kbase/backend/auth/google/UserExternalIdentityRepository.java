package com.kbase.backend.auth.google;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserExternalIdentityRepository extends JpaRepository<UserExternalIdentity, UUID> {
    Optional<UserExternalIdentity> findByProviderAndProviderSubject(ExternalIdentityProvider provider, String providerSubject);
}
