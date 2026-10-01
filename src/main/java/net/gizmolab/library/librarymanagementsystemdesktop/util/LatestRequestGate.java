package net.gizmolab.library.librarymanagementsystemdesktop.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Search-as-you-type: each request takes a token; only the answer of the latest request may update the UI,
 * so a slow answer for "Λο" cannot overwrite the answer for "Λοϊζ".
 */
public final class LatestRequestGate {

    private final AtomicLong latest = new AtomicLong();

    public long next() {
        return latest.incrementAndGet();
    }

    public boolean isLatest(long token) {
        return latest.get() == token;
    }
}
