package com.nemouw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Config {
    private static final Logger logger = LogManager.getLogger(Config.class);

    @Getter
    private final static int ddosShieldRequestLimitPerSecond;

    static {
        ObjectMapper mapper = new ObjectMapper();
        File configFile = new File("config.json");
        JsonNode config = mapper.createObjectNode();

        if (!configFile.exists()) {
            logger.warn("Configuration file 'config.json' not found. Falling back to default system values.");
        } else {
            try {
                config = mapper.readTree(configFile);
            } catch (Exception e) {
                logger.error("Failed to parse 'config.json'. Falling back to default system values.", e);
            }
        }

        ddosShieldRequestLimitPerSecond = config.path("ddosShieldRequestLimitPerSecond").asInt(5);

        logger.info("Configuration loaded. DDoS shield request limit set to: {} req/sec.", ddosShieldRequestLimitPerSecond);
    }
}