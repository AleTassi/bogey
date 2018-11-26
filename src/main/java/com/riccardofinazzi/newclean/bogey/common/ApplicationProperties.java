package com.riccardofinazzi.newclean.bogey.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class ApplicationProperties {

    private static final Logger log = LoggerFactory.getLogger(ApplicationProperties.class);

    private static ApplicationProperties applicationProperties;
    private static Properties properties;

    private ApplicationProperties() {
    }

    public static ApplicationProperties getInstance() {
        return null == applicationProperties ? applicationProperties = new ApplicationProperties() : applicationProperties;
    }

    public void init() {

        properties = new Properties();

        try (InputStream is = getClass().getResourceAsStream("/app.properties");) {
            log.info("Loading properties from classpath: /app.properties");
            properties.load(is);

            log.info("{}, version {}", properties.get("name"), properties.get("version"));
        } catch (Exception e) {
            log.error("Error loading properties: " + e.getMessage(), e);
            throw new RuntimeException("Error loading properties: " + e.getMessage(), e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalStateException(String.format("Missing property: %s", key));
        }
        return value;
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

}
