package ch.so.agi.ilivalidator.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ch.so.agi.ilivalidator.Settings;
import ch.so.agi.ilivalidator.service.StorageService;
import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
public class MainController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.workDirectory}")
    private String workDirectory;
    
    @Value("${app.folderPrefix}")
    private String folderPrefix;
    
    @Autowired
    Settings settings;
    
    private StorageService storageService;

    public MainController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostConstruct
    public void init() throws Exception {
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return new ResponseEntity<String>("ilivalidatorws", HttpStatus.OK);
    }
    
    @GetMapping("/logs/{key}/{filename}") 
    public ResponseEntity<?> getLog(@PathVariable String key, @PathVariable String filename) throws IOException {        
        MediaType mediaType = new MediaType("text", "plain", StandardCharsets.UTF_8);
        if (filename.endsWith(".xtf")) {
            mediaType = MediaType.parseMediaType(MediaType.APPLICATION_XML_VALUE);
        }
        
        try {
            Path logFile = storageService.load(key + "/" + filename);

            // InputStream is = Files.newInputStream(logFile);
            // Resolved [org.springframework.http.converter.HttpMessageNotWritableException: No converter for [class software.amazon.awssdk.core.ResponseInputStream] with preset Content-Type 'text/plain;charset=UTF-8']
            
            return ResponseEntity.ok().header("Content-Type", "charset=utf-8")
                    .contentLength(Files.size(logFile))
                    .contentType(mediaType)
                    .body(Files.readString(logFile));

        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);  
        }
    }  
}
