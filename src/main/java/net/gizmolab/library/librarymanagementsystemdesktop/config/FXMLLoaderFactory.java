package net.gizmolab.library.librarymanagementsystemdesktop.config;

import net.gizmolab.library.librarymanagementsystemdesktop.service.I18nManager;
import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

/**
 * Factory class for creating FXMLLoader instances with Spring dependency injection support.
 * This enables JavaFX controllers to receive Spring-managed beans through constructor injection.
 */
@Component
public class FXMLLoaderFactory {

    private final ApplicationContext applicationContext;

    public FXMLLoaderFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Creates an FXMLLoader with Spring-based controller factory.
     * 
     * @param fxmlResource the URL of the FXML resource to load
     * @return configured FXMLLoader instance
     */
    public FXMLLoader createLoader(URL fxmlResource) {
        FXMLLoader loader = new FXMLLoader(fxmlResource);
        loader.setControllerFactory(applicationContext::getBean);
        return loader;
    }

    /**
     * Creates an FXMLLoader with Spring-based controller factory.
     * 
     * @param fxmlPath the classpath path to the FXML resource
     * @return configured FXMLLoader instance
     */
    public FXMLLoader createLoader(String fxmlPath) {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IllegalArgumentException("FXML resource not found: " + fxmlPath);
        }
        return createLoader(resource);
    }

    /**
     * Loads an FXML view and returns the root node.
     * 
     * @param fxmlPath the classpath path to the FXML resource
     * @param <T> the type of the root node
     * @return the loaded root node
     * @throws IOException if the FXML cannot be loaded
     */
    public <T> T load(String fxmlPath) throws IOException {
        FXMLLoader loader = createLoader(fxmlPath);
        
        // Set resource bundle for internationalization
        try {
            // Try to get current locale from I18nManager if available
            java.util.Locale currentLocale = java.util.Locale.getDefault();
            try {
                // Try to get I18nManager from application context
                I18nManager i18nManager =
                    applicationContext.getBean(I18nManager.class);
                currentLocale = i18nManager.getCurrentLocale();
            } catch (Exception e) {
                // Fall back to system default if I18nManager is not available
            }
            
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("messages", currentLocale);
            loader.setResources(bundle);
        } catch (Exception e) {
            // If resource bundle is not found, continue without it
            System.out.println("Warning: Could not load resource bundle 'messages': " + e.getMessage());
        }
        
        return loader.load();
    }

    /**
     * Loads an FXML view and returns both the root node and the controller.
     * 
     * @param fxmlPath the classpath path to the FXML resource
     * @param <T> the type of the root node
     * @param <C> the type of the controller
     * @return a LoadResult containing both root and controller
     * @throws IOException if the FXML cannot be loaded
     */
    public <T, C> LoadResult<T, C> loadWithController(String fxmlPath) throws IOException {
        FXMLLoader loader = createLoader(fxmlPath);
        
        // Set resource bundle for internationalization
        try {
            // Try to get current locale from I18nManager if available
            java.util.Locale currentLocale = java.util.Locale.getDefault();
            try {
                // Try to get I18nManager from application context
                I18nManager i18nManager =
                    applicationContext.getBean(I18nManager.class);
                currentLocale = i18nManager.getCurrentLocale();
            } catch (Exception e) {
                // Fall back to system default if I18nManager is not available
            }
            
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("messages", currentLocale);
            loader.setResources(bundle);
        } catch (Exception e) {
            // If resource bundle is not found, continue without it
            System.out.println("Warning: Could not load resource bundle 'messages': " + e.getMessage());
        }
        
        T root = loader.load();
        C controller = loader.getController();
        return new LoadResult<>(root, controller);
    }

    /**
     * Result class that holds both the loaded root node and its controller.
     */
    public static class LoadResult<T, C> {
        private final T root;
        private final C controller;

        public LoadResult(T root, C controller) {
            this.root = root;
            this.controller = controller;
        }

        public T getRoot() {
            return root;
        }

        public C getController() {
            return controller;
        }
    }
}