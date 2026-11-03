package com.nemouw.media.storage;

import com.nemouw.media.MediaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
class S3Config {

    @Bean(destroyMethod = "close")
    S3Client s3Client(MediaProperties props) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(props.region()));
        if (props.hasEndpoint()) {
            builder.endpointOverride(URI.create(props.endpoint())).forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner(MediaProperties props) {
        S3Presigner.Builder builder = S3Presigner.builder().region(Region.of(props.region()));
        if (props.hasEndpoint()) {
            builder.endpointOverride(URI.create(props.endpoint()))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }
}