package net.gizmolab.library.librarymanagementsystemdesktop.service;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for creating consistent animations throughout the application.
 * Provides static methods for common UI animations with easing functions.
 * Monitors animation performance and automatically disables animations if FPS drops below threshold.
 */
public class AnimationHelper {
    private static final Logger logger = LoggerFactory.getLogger(AnimationHelper.class);
    private static final int MIN_FPS_THRESHOLD = 20;
    private static final int TARGET_FPS = 30;
    private static boolean animationsEnabled = true;
    private static PerformanceMonitor performanceMonitor = new PerformanceMonitor();

    /**
     * Applies an opening animation to a dialog (fade in + scale up).
     * The dialog scales from 0.8 to 1.0 while fading in.
     *
     * @param dialog the dialog to animate
     */
    public static void dialogOpenAnimation(Dialog<?> dialog) {
        Node dialogPane = dialog.getDialogPane();
        Duration duration = Duration.millis(250);
        
        if (!animationsEnabled) {
            dialogPane.setOpacity(1.0);
            dialogPane.setScaleX(1.0);
            dialogPane.setScaleY(1.0);
            return;
        }
        
        // Set initial state
        dialogPane.setOpacity(0.0);
        dialogPane.setScaleX(0.8);
        dialogPane.setScaleY(0.8);
        
        performanceMonitor.startMonitoring();
        
        // Fade in
        FadeTransition fade = new FadeTransition(duration, dialogPane);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);
        
        // Scale up
        ScaleTransition scale = new ScaleTransition(duration, dialogPane);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);
        scale.setOnFinished(e -> performanceMonitor.stopMonitoring());
        
        fade.play();
        scale.play();
    }

    /**
     * Inner class for monitoring animation performance.
     * Tracks frame rate and automatically disables animations if performance degrades.
     */
    private static class PerformanceMonitor {
        private AnimationTimer timer;
        private long lastFrameTime = 0;
        private int frameCount = 0;
        private double totalFPS = 0;
        private int measurementCount = 0;
        private boolean isMonitoring = false;
        
        /**
         * Start monitoring frame rate.
         */
        public void startMonitoring() {
            if (isMonitoring) {
                return;
            }
            
            isMonitoring = true;
            frameCount = 0;
            lastFrameTime = System.nanoTime();
            
            timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (lastFrameTime > 0) {
                        long elapsedNanos = now - lastFrameTime;
                        double fps = 1_000_000_000.0 / elapsedNanos;
                        
                        frameCount++;
                        totalFPS += fps;
                        measurementCount++;
                        
                        // Check if FPS drops below threshold
                        if (frameCount > 10 && fps < MIN_FPS_THRESHOLD) {
                            logger.warn("Animation FPS dropped to {}, below threshold of {}. Disabling animations.", 
                                    String.format("%.1f", fps), MIN_FPS_THRESHOLD);
                            animationsEnabled = false;
                            stopMonitoring();
                        }
                    }
                    lastFrameTime = now;
                }
            };
            
            timer.start();
        }
        
        /**
         * Stop monitoring frame rate.
         */
        public void stopMonitoring() {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            isMonitoring = false;
            
            // Log performance metrics
            if (measurementCount > 0) {
                double avgFPS = totalFPS / measurementCount;
                logger.debug("Animation completed. Average FPS: {}", String.format("%.1f", avgFPS));
                
                if (avgFPS >= TARGET_FPS) {
                    logger.debug("Animation performance meets target FPS of {}", TARGET_FPS);
                } else if (avgFPS >= MIN_FPS_THRESHOLD) {
                    logger.debug("Animation performance acceptable (above {} FPS threshold)", MIN_FPS_THRESHOLD);
                }
            }
        }
        
    }
}
