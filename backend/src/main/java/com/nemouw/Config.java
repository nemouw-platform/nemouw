package com.nemouw;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;

public class Config {
    private static final Logger logger = LogManager.getLogger(Config.class);

    @Getter
    private static int ddosShieldRequestLimitPerSecond;

    static {
        File configFile = new File("config.json");
        JSONObject config = new JSONObject();

        if (!configFile.exists()) {
            logger.warn("Configuration file 'config.json' not found. Falling back to default system values.");
        } else {
            try {
                config = new JSONObject(Files.readString(configFile.toPath()));
            } catch (Exception e) {
                logger.error("Failed to parse 'config.json'. Falling back to default system values.", e);
            }
        }

        ddosShieldRequestLimitPerSecond = config.optInt("ddosShieldRequestLimitPerSecond", 5);

        logger.info("Configuration loaded. DDoS shield request limit set to: {} req/sec.", ddosShieldRequestLimitPerSecond);
    }
}
