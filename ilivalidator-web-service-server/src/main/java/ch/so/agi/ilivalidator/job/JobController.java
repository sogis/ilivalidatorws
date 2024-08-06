package ch.so.agi.ilivalidator.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ch.so.agi.ilivalidator.profile.ProfileProperties;
import ch.so.agi.ilivalidator.storage.StorageService;

@ConditionalOnProperty(
        value="app.restApiEnabled", 
        havingValue = "true", 
        matchIfMissing = false)
@RestController
public class JobController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private static final String PATH_TO_LOG_API = "api/logs";
    
    @Value("${app.folderPrefix}")
    private String folderPrefix;
    
    private Map<String,String> profiles;

    private StorageService storageService;
    
    private JobScheduler jobScheduler;
    
    private JobService jobService;
    
    private StorageProvider storageProvider;

    public JobController(
            @Value("${app.folderPrefix}") String folderPrefix,
            ProfileProperties profileProperties, 
            StorageService storageService, 
            JobScheduler jobScheduler, 
            JobService jobService,
            StorageProvider storageProvider) {
        this.folderPrefix = folderPrefix;
        this.profiles = profileProperties.getProfiles();
        this.storageService = storageService;
        this.jobScheduler = jobScheduler;
        this.jobService = jobService;
        this.storageProvider = storageProvider;
    }

    // @RequestPart anstelle von @RequestParam und @RequestBody damit swagger korrekt funktioniert.
    @PostMapping(value="/api/jobs", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadFiles(
            @RequestPart(name="files", required=true) MultipartFile[] files, 
            @RequestPart(name="profile", required=false) String profile) {
        
        String jobId = UUID.randomUUID().toString();
        String profileString = profile==null?"":profile;
        
        log.debug("<{}> Selected profile: {}", jobId, profile);
        log.debug("<{}> Number of uploaded files: {}", jobId, files.length);

        
        Path[] uploadedFiles;
        try {
            uploadedFiles = storageService.store(files, jobId);
        } catch (IOException e) {
            throw new RuntimeException("Could not store files.");
        }

        jobScheduler.enqueue(UUID.fromString(jobId), () -> jobService.validate(JobContext.Null, uploadedFiles, profileString));
        
        return ResponseEntity
                .accepted()
                .header("Operation-Location", getHost()+"/api/jobs/"+jobId)
                .body(null);        
    }
    
    @GetMapping("/api/jobs/{jobId}")
    public ResponseEntity<?> getJobById(@PathVariable("jobId") String jobId) throws IOException {
        try {
            Job job = storageProvider.getJobById(UUID.fromString(jobId));        
    
            Path logFile = storageService.load(folderPrefix + jobId + "/" + jobId + ".log");
            
            String logFileLocation = null;
            String xtfLogFileLocation = null;
            String csvLogFileLocation = null;
            String validationResult = null;
            if (job.getJobState().getName().equals(StateName.SUCCEEDED)) {
                logFileLocation = getHost() + "/" + PATH_TO_LOG_API + "/" + folderPrefix + jobId + "/" + jobId + ".log";
                xtfLogFileLocation = logFileLocation + ".xtf"; 
                csvLogFileLocation = logFileLocation + ".csv"; 
                try {
                    // JobResult nur bei Pro-Version. Darum handgestrickt.
                    String content = Files.readString(logFile);
                    if (content.contains("...validation done")) {
                        validationResult = ValidationResult.SUCCEEDED.toString();
                    } else if (content.contains("...validation failed")) {
                        validationResult = ValidationResult.FAILED.toString();
                    } else if (content.contains("Error")) {
                        validationResult = ValidationResult.FAILED.toString();                    
                    } else {
                        validationResult = ValidationResult.UNKNOWN.toString();
                    }
                } catch (IOException e) {
                    validationResult = ValidationResult.UNKNOWN.toString();
                    e.printStackTrace();
                }
            } else {
                log.debug("<{}> Status request from client: {}", jobId, job.getJobState().getName());
            }
            
            String message = null;
            JobResponse jobResponse = new JobResponse(
                    LocalDateTime.ofInstant(job.getCreatedAt(), ZoneId.systemDefault()),
                    LocalDateTime.ofInstant(job.getUpdatedAt(), ZoneId.systemDefault()), 
                    job.getState().name(),
                    message,
                    validationResult, 
                    logFileLocation, 
                    xtfLogFileLocation,
                    csvLogFileLocation
                    );
    
            if (jobResponse.jobStatus().equals(StateName.SUCCEEDED.toString())) {
                return ResponseEntity.ok().body(jobResponse);
            } else if (jobResponse.jobStatus().equals(StateName.FAILED.toString())) {
                // Jobrunr-Api erlaubt aus mir nicht auf den Stacktrace zuzugreifen, der aufgetreten ist.
                // Eventuell ginge es mit der Pro-Version.
                return ResponseEntity.badRequest().body(jobResponse); // 400
            } else {
                return ResponseEntity.ok().header("Retry-After", "30").body(jobResponse);            
            }
        } catch (JobNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.noContent().build();
        }
    }

    private String getHost() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
    
    private static enum ValidationResult {
        SUCCEEDED,
        FAILED,
        UNKNOWN
      }

}
