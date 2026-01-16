package ch.so.agi.ilivalidator.worker;

/**
 * Status information about JobRunr workers.
 *
 * @param isAvailable true if at least one worker is active and ready
 * @param activeWorkerCount number of workers with recent heartbeat
 * @param message human-readable status message
 */
public record WorkerStatus(
    boolean isAvailable,
    int activeWorkerCount,
    String message
) {}
