package com.kbase.backend.auth.google;

public record VerifiedGoogleIdentity(String subject, String email, boolean emailVerified, String name, String picture) {}
