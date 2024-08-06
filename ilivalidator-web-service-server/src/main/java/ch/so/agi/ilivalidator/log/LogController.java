package ch.so.agi.ilivalidator.log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ch.so.agi.ilivalidator.storage.StorageService;

@ConditionalOnProperty(
        value="app.restApiEnabled", 
        havingValue = "true", 
        matchIfMissing = false)
@RestController
public class LogController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private StorageService storageService;

    public LogController(StorageService storageService) {
        this.storageService = storageService;
    }
    
    @GetMapping("/api/logs/{key}/{filename}") 
    public ResponseEntity<?> getLog(@PathVariable("key") String key, @PathVariable("filename") String filename) throws IOException {        
        MediaType mediaType = new MediaType("text", "plain", StandardCharsets.UTF_8);
        if (filename.endsWith(".xtf")) {
            mediaType = MediaType.parseMediaType(MediaType.APPLICATION_XML_VALUE);
        }
        
        try {
            Path logFile = storageService.load(key + "/" + filename);
            return ResponseEntity.ok().header("Content-Type", "charset=utf-8")
                    .contentLength(Files.size(logFile))
                    .contentType(mediaType)
                    .body(Files.readString(logFile));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
