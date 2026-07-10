package net.gizmolab.library.librarymanagementsystemdesktop.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration class for JavaFX-specific Spring Boot settings.
 * This configuration is activated when running in JavaFX mode.
 */
@Configuration
public class JavaFXConfig {

    /**
     * This configuration class ensures that JavaFX-related beans are properly configured
     * and that the Spring Boot context is optimized for desktop application usage.
     * 
     * Additional JavaFX-specific configurations can be added here as needed.
     */
    
    // Future configurations for JavaFX-specific beans can be added here
    // For example: custom converters, formatters, or JavaFX-specific services
}