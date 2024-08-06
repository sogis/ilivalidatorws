package ch.so.agi.ilivalidator.cleaner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import ch.so.agi.ilivalidator.storage.LocalFileStorageService;
import ch.so.agi.ilivalidator.storage.StorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ConditionalOnProperty(
        value="app.cleanerEnabled", 
        havingValue = "true", 
        matchIfMissing = false)
@Service
public class CleanerService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private StorageService storageService;
    
    private String workDirectory;
    
    private String folderPrefix;

    public CleanerService(
            @Value("${app.workDirectory}") String workDirectory,
            @Value("${app.folderPrefix}") String folderPrefix,
            StorageService storageService) {
        this.storageService = storageService;
        this.workDirectory = workDirectory;
        this.folderPrefix = folderPrefix;
    }
    
    @Async("cleanerAsyncTaskExecutor")
    @Scheduled(cron="0 */30 * * * *")
    //@Scheduled(fixedRate = 1 * 30 * 1000) /* Runs every 30 seconds */
    //@Scheduled(fixedRate = 1 * 10 * 1000) /* Runs every 10 seconds */
    public void cleanUp() throws IOException {    
        long deleteFileAge = 60*60*24; // = 1 Tag
        log.info("Deleting files from previous delivery runs older than {} [s]...", deleteFileAge);
        if (storageService instanceof LocalFileStorageService) {
            Files.list(Paths.get(workDirectory)).forEach(d -> {                
                if (d.getName(d.getNameCount()-1).toString().startsWith(folderPrefix)) {
                    try {
                        FileTime creationTime = (FileTime) Files.getAttribute(d, "creationTime");                    
                        Instant now = Instant.now();
                        
                        long fileAge = now.getEpochSecond() - creationTime.toInstant().getEpochSecond();
                        log.trace("Found folder with prefix: {}, age [s]: {}", d, fileAge);

                        if (fileAge > deleteFileAge) {
                            log.debug("Deleting {}", d.toAbsolutePath());
                            FileSystemUtils.deleteRecursively(d);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("Error deleting old files: " + e.getMessage());
                    }
                }
            });            
        }
    }
}
