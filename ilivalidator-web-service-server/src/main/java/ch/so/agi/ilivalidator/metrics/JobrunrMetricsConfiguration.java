package ch.so.agi.ilivalidator.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Exposes Jobrunr metrics to Prometheus via Spring Boot Actuator.
 *
 * This configuration registers custom metrics that track the number of jobs
 * in various states, which can be used for monitoring and autoscaling.
 */
@Configuration
public class JobrunrMetricsConfiguration {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public JobrunrMetricsConfiguration(MeterRegistry registry, StorageProvider storageProvider) {
        log.info("Registering Jobrunr metrics for Prometheus");

        // Total pending jobs (SCHEDULED + ENQUEUED)
        Gauge.builder("jobrunr_pending_jobs", storageProvider, sp -> {
                try {
                    JobStats stats = sp.getJobStats();
                    long scheduled = stats.getScheduled() != null ? stats.getScheduled() : 0L;
                    long enqueued = stats.getEnqueued() != null ? stats.getEnqueued() : 0L;
                    return (double) (scheduled + enqueued);
                } catch (Exception e) {
                    log.error("Error getting pending jobs stats", e);
                    return 0.0;
                }
            })
            .description("Number of pending Jobrunr jobs (SCHEDULED + ENQUEUED)")
            .register(registry);

        // Processing jobs
        Gauge.builder("jobrunr_processing_jobs", storageProvider, sp -> {
                try {
                    JobStats stats = sp.getJobStats();
                    Long processing = stats.getProcessing();
                    return processing != null ? processing.doubleValue() : 0.0;
                } catch (Exception e) {
                    log.error("Error getting processing jobs stats", e);
                    return 0.0;
                }
            })
            .description("Number of currently processing Jobrunr jobs")
            .register(registry);

        // Succeeded jobs (total)
        Gauge.builder("jobrunr_succeeded_jobs", storageProvider, sp -> {
                try {
                    JobStats stats = sp.getJobStats();
                    Long succeeded = stats.getSucceeded();
                    return succeeded != null ? succeeded.doubleValue() : 0.0;
                } catch (Exception e) {
                    log.error("Error getting succeeded jobs stats", e);
                    return 0.0;
                }
            })
            .description("Total number of succeeded Jobrunr jobs")
            .register(registry);

        // Failed jobs (total)
        Gauge.builder("jobrunr_failed_jobs", storageProvider, sp -> {
                try {
                    JobStats stats = sp.getJobStats();
                    Long failed = stats.getFailed();
                    return failed != null ? failed.doubleValue() : 0.0;
                } catch (Exception e) {
                    log.error("Error getting failed jobs stats", e);
                    return 0.0;
                }
            })
            .description("Total number of failed Jobrunr jobs")
            .register(registry);

        // Active workers (registered BackgroundJobServers with recent heartbeat)
        Gauge.builder("jobrunr_active_workers", storageProvider, sp -> {
                try {
                    List<BackgroundJobServerStatus> servers = sp.getBackgroundJobServers();
                    if (servers == null || servers.isEmpty()) {
                        return 0.0;
                    }

                    // Count workers with heartbeat in last 30 seconds
                    Instant now = Instant.now();
                    long activeWorkers = servers.stream()
                        .filter(server -> {
                            Instant lastHeartbeat = server.getLastHeartbeat();
                            long secondsSinceHeartbeat = ChronoUnit.SECONDS.between(lastHeartbeat, now);
                            return secondsSinceHeartbeat < 30;
                        })
                        .count();

                    return (double) activeWorkers;
                } catch (Exception e) {
                    log.error("Error getting active workers count", e);
                    return 0.0;
                }
            })
            .description("Number of active Jobrunr workers (BackgroundJobServers with recent heartbeat)")
            .register(registry);

        log.info("Jobrunr metrics registered successfully");
    }
}
