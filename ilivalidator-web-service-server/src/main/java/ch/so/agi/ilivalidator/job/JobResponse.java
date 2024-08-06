package ch.so.agi.ilivalidator.job;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobResponse(
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String jobStatus,
            String message,
            String validationStatus,
            String logFileLocation,
            String xtfLogFileLocation,
            String csvLogFileLocation
        ) {}
