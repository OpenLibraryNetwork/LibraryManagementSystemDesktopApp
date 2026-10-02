package net.gizmolab.library.librarymanagementsystemdesktop.testsupport;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * JavaFX can start only once per JVM and cannot restart after Platform.exit().
 * Every test that needs the toolkit calls start(); nothing calls Platform.exit().
 */
public final class FxToolkit {

    private static boolean started;

    private FxToolkit() {}

    public static synchronized void start() throws InterruptedException {
        if (started) return;
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException _) {
            latch.countDown();
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX did not start within 10 seconds");
        }
        Platform.setImplicitExit(false);
        started = true;
    }
}
