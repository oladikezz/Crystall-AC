package com.crystallac.ml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Ring buffer holding historical feature snapshots for the last 1-5 minutes (configurable).
 * Automatically purges stale snapshots.
 */
public class FeatureBuffer {

    private final long maxDurationMs;
    private final Deque<FeatureSnapshot> snapshots = new ArrayDeque<>();

    public FeatureBuffer(int durationSeconds) {
        this.maxDurationMs = (long) durationSeconds * 1000L;
    }

    public synchronized void addSnapshot(FeatureSnapshot snapshot) {
        long now = System.currentTimeMillis();
        snapshots.addLast(snapshot);

        // Purge expired records
        while (!snapshots.isEmpty() && (now - snapshots.peekFirst().getTimestamp()) > maxDurationMs) {
            snapshots.pollFirst();
        }
    }

    public synchronized List<FeatureSnapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }

    public synchronized int size() {
        return snapshots.size();
    }

    public synchronized void clear() {
        snapshots.clear();
    }
}
