package com.noura.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.media")
public class MediaStorageProperties {

    private String uploadDir = System.getProperty("user.home") + "/.noura/uploads";
    private String publicPathPrefix = "/uploads";
    private long maxBytes = 8_000_000;
    private boolean allowSvg = false;
    private int importTimeoutMs = 6_000;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getPublicPathPrefix() {
        return publicPathPrefix;
    }

    public void setPublicPathPrefix(String publicPathPrefix) {
        this.publicPathPrefix = publicPathPrefix;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public boolean isAllowSvg() {
        return allowSvg;
    }

    public void setAllowSvg(boolean allowSvg) {
        this.allowSvg = allowSvg;
    }

    public int getImportTimeoutMs() {
        return importTimeoutMs;
    }

    public void setImportTimeoutMs(int importTimeoutMs) {
        this.importTimeoutMs = importTimeoutMs;
    }
}
