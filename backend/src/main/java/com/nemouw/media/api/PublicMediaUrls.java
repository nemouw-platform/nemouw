package com.nemouw.media.api;

import com.nemouw.media.MediaProperties;
import org.springframework.stereotype.Component;

@Component
public class PublicMediaUrls {
    private final String base;

    PublicMediaUrls(MediaProperties props) {
        String url = props.publicBaseUrl();
        this.base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String urlFor(String s3Key) {
        return s3Key == null ? null : base + "/" + s3Key;
    }
}