package com.zhai.hw.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zhai.hw.config.AppProperties;
import com.zhai.hw.exception.InvalidConfigurationException;
import com.zhai.hw.model.AppConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfigurationLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLoaderService.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ConfigurationLoaderService(AppProperties appProperties, Validator validator) {
        this.appProperties = appProperties;
        this.validator = validator;
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Loads configuration from the path specified in AppProperties.
     * 
     * @return The loaded and validated AppConfig object
     * @throws InvalidConfigurationException if the configuration is invalid or cannot be loaded
     */
    public AppConfig loadConfiguration() {
        String configPath = appProperties.getConfigPath();
        return loadConfiguration(configPath);
    }

    /**
     * Loads configuration from the specified path.
     * 
     * @param filePath The path to the configuration file
     * @return The loaded and validated AppConfig object
     * @throws InvalidConfigurationException if the configuration is invalid or cannot be loaded
     */
    public AppConfig loadConfiguration(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new InvalidConfigurationException("Configuration file path is not defined.");
        }

        logger.debug("Loading configuration from: {}", filePath);

        try (InputStream inputStream = getInputStream(filePath)) {
            if (inputStream == null) {
                throw new InvalidConfigurationException("Cannot find configuration file at: " + filePath);
            }
            AppConfig appConfig = objectMapper.readValue(inputStream, AppConfig.class);
            validateConfig(appConfig);
            logger.info("Successfully loaded configuration for application: {}", appConfig.getAppName());
            return appConfig;
        } catch (JsonProcessingException e) {
            // Log at debug level to avoid cluttering test output with expected errors
            logger.debug("Error parsing YAML configuration file: {}", filePath, e);
            throw new InvalidConfigurationException("Error parsing YAML configuration file: " + filePath, e);
        } catch (IOException e) {
            // Log at debug level to avoid cluttering test output with expected errors
            logger.debug("Error reading configuration file: {}", filePath, e);
            throw new InvalidConfigurationException("Error reading configuration file: " + filePath, e);
        }
    }

    private InputStream getInputStream(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String resourcePath = path.substring("classpath:".length());
            logger.debug("Loading from classpath: {}", resourcePath);
            return new ClassPathResource(resourcePath).getInputStream();
        } else if (path.startsWith("file:")) {
            String filePath = path.substring("file:".length());
            logger.debug("Loading from file system: {}", filePath);
            return new FileSystemResource(filePath).getInputStream();
        }
        // Default to classpath if no prefix
        logger.debug("No prefix specified, defaulting to classpath: {}", path);
        return new ClassPathResource(path).getInputStream();
    }

    private void validateConfig(AppConfig appConfig) {
        Set<ConstraintViolation<AppConfig>> violations = validator.validate(appConfig);
        if (!violations.isEmpty()) {
            String errorMessages = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            // Log at debug level to avoid cluttering test output with expected errors
            logger.debug("Configuration validation failed: {}", errorMessages);
            throw new InvalidConfigurationException("Invalid configuration: " + errorMessages);
        }
    }
}
