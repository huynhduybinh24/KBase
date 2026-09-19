package com.kbase.backend.auth.google;

public interface GoogleIdentityVerifier {
    VerifiedGoogleIdentity verify(String credential);
}
