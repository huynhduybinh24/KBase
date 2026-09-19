package com.kbase.backend.user.profile;

import java.io.InputStream;

public record AvatarDownload(InputStream content, String contentType, long size) {}
