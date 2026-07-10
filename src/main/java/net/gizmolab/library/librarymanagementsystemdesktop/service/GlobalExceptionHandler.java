package net.gizmolab.library.librarymanagementsystemdesktop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

/**
 * Global exception handler stub.
 * Provides basic exception handling for controllers.
 * TODO: Full rewrite in Batch 6.
 */
@Service
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public void handleException(String operation, Exception e) {
        logger.error("Error during {}: {}", operation, e.getMessage(), e);
    }

    public void executeWithExceptionHandling(String operation, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            handleException(operation, e);
        }
    }

    public <T> T executeWithExceptionHandling(String operation, Callable<T> task, T defaultValue) {
        try {
            return task.call();
        } catch (Exception e) {
            handleException(operation, e);
            return defaultValue;
        }
    }
}
