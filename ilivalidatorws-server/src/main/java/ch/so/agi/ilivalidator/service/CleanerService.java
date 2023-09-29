package ch.so.agi.ilivalidator.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

@ConditionalOnProperty(
        value="app.cleanerEnabled", 
        havingValue = "true", 
        matchIfMissing = false)
@Service
public class CleanerService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private StorageService storageService;

    @Value("${app.workDirectory}")
    private String workDirectory;
    
    @Value("${app.folderPrefix}")
    private String folderPrefix;
       
    public CleanerService(StorageService storageService) {
        this.storageService = storageService;
    }
    
    /*
     * Verzeichnisse löschen, die älter als 60x60 Sekunden alt sind.
     */
    //@Scheduled(cron="0 0/2 * * * ?")
    @Scheduled(cron="0 * * * * *")
    private void cleanUp() {    
        // TODO: Geht das eleganter?
        // workDirectory ist der Bucket. Müsste anders gemacht werden.
        long deleteFileAge = 60*60;
        log.info("Deleting files from previous validation runs older than {} [s] ...", deleteFileAge);
        if (storageService instanceof LocalStorageService) {
            java.io.File[] tmpDirs = new java.io.File(workDirectory).listFiles();
            if(tmpDirs!=null) {
                for (java.io.File tmpDir : tmpDirs) {
                    if (tmpDir.getName().startsWith(folderPrefix)) {
                        try {
                            FileTime creationTime = (FileTime) Files.getAttribute(Paths.get(tmpDir.getAbsolutePath()), "creationTime");                    
                            Instant now = Instant.now();
                            
                            long fileAge = now.getEpochSecond() - creationTime.toInstant().getEpochSecond();
                            log.debug("found folder with prefix: {}, age [s]: {}", tmpDir, fileAge);

                            if (fileAge > deleteFileAge) {
                                log.debug("deleting {}", tmpDir.getAbsolutePath());
                                FileSystemUtils.deleteRecursively(tmpDir);
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
            }            
        } 
    }
}
