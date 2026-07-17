package com.queuectl.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.queuectl.exception.DatabaseException;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = createMapper();

    private JsonUtil() {}

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new DatabaseException("Failed to serialize object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new DatabaseException("Failed to deserialize JSON: " + json, e);
        }
    }

    public static String prettyPrint(Object object) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return toJson(object);
        }
    }
}
