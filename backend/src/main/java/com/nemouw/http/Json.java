package com.nemouw.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nemouw.error.Errors;
import com.nemouw.model.AuthDtos;

public final class Json {
    private Json() {}

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static String write(Object o) {
        if (o instanceof AuthDtos.IssuedSession) {
            throw new IllegalStateException("IssuedSession contains secrets and must never be serialized");
        }

        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    public static <T> T read(String body, Class<T> type) {
        if (body == null || body.isBlank()) throw Errors.malformedJson("Request body is required");
        try {
            return MAPPER.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw Errors.malformedJson("Request body is not valid JSON");
        }
    }
}