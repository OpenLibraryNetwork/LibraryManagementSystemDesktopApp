package net.gizmolab.library.librarymanagementsystemdesktop.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Runs a call off the FX thread; both callbacks run on the FX thread. */
public final class BackgroundTasks {

    private BackgroundTasks() {}

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        Thread thread = new Thread(task, "catalog-task");
        thread.setDaemon(true);
        thread.start();
    }
}
