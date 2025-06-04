package com.zhai.hw;

import com.zhai.hw.exception.InvalidConfigurationException;
import com.zhai.hw.model.AppConfig;
import com.zhai.hw.service.ConfigurationLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class HwApplication {

	private static final Logger logger = LoggerFactory.getLogger(HwApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(HwApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx, ConfigurationLoaderService configurationLoaderService) {
		return args -> {
			try {
				logger.info("Attempting to load application configuration...");
				AppConfig appConfig = configurationLoaderService.loadConfiguration();
				// Optionally, log some loaded configuration details or store it for application-wide access
				logger.info("Successfully loaded application configuration. App Name: {}", appConfig.getAppName());
				logger.info("Server configuration: host={}, port={}", appConfig.getServer().getHost(), appConfig.getServer().getPort());
				logger.info("VM Pool configuration: maxSize={}, idleTimeout={}, defaultOs={}", 
					appConfig.getVmPool().getMaxSize(), 
					appConfig.getVmPool().getIdleTimeout(), 
					appConfig.getVmPool().getDefaultOs());
				logger.info("Number of label VM mappings: {}", appConfig.getLabelVmMappings().size());
				// You can store the loaded AppConfig in a bean or make it accessible globally if needed
			} catch (InvalidConfigurationException e) {
				logger.error("Failed to load application configuration: {}", e.getMessage(), e);
				// Exit the application
				SpringApplication.exit(ctx, () -> 1);
			} catch (Exception e) {
				logger.error("An unexpected error occurred during application startup while loading configuration: {}", e.getMessage(), e);
				SpringApplication.exit(ctx, () -> 1);
			}
		};
	}
	
	@Bean
	@Primary
	public AppConfig appConfig(ConfigurationLoaderService configurationLoaderService) {
		return configurationLoaderService.loadConfiguration();
	}
}
