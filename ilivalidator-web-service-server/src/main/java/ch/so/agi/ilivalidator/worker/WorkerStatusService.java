package ch.so.agi.ilivalidator.worker;

import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service to check if JobRunr workers are available and ready to process jobs.
 *
 * This is especially useful in KEDA scale-to-zero scenarios where workers
 * may need 5-10 seconds to start up after a job is enqueued.
 */
@Service
public class WorkerStatusService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final StorageProvider storageProvider;

    public WorkerStatusService(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    /**
     * Check if any JobRunr workers are currently running and ready.
     *
     * @return WorkerStatus with availability information
     */
    public WorkerStatus getWorkerStatus() {
        try {
            List<BackgroundJobServerStatus> servers = storageProvider.getBackgroundJobServers();

            if (servers == null || servers.isEmpty()) {
                log.debug("No workers registered");
                return new WorkerStatus(false, 0, "No workers available - scaling up may be in progress");
            }

            // Count active workers (last seen within 30 seconds)
            Instant now = Instant.now();
            long activeWorkers = servers.stream()
                .filter(server -> {
                    Instant lastHeartbeat = server.getLastHeartbeat();
                    long secondsSinceHeartbeat = ChronoUnit.SECONDS.between(lastHeartbeat, now);
                    return secondsSinceHeartbeat < 30; // Worker is active if heartbeat < 30s ago
                })
                .count();

            if (activeWorkers == 0) {
                log.debug("Workers registered but not active (no recent heartbeat)");
                return new WorkerStatus(false, 0, "Workers starting up - please wait");
            }

            log.debug("Found {} active worker(s)", activeWorkers);
            return new WorkerStatus(true, (int) activeWorkers, "Workers ready");

        } catch (Exception e) {
            log.error("Error checking worker status", e);
            return new WorkerStatus(false, 0, "Error checking worker status: " + e.getMessage());
        }
    }

    /**
     * Check if workers are ready (simple boolean).
     *
     * @return true if at least one worker is active
     */
    public boolean areWorkersReady() {
        return getWorkerStatus().isAvailable();
    }

    /**
     * Get the number of active workers.
     *
     * @return number of workers with recent heartbeat
     */
    public int getActiveWorkerCount() {
        return getWorkerStatus().activeWorkerCount();
    }
}
