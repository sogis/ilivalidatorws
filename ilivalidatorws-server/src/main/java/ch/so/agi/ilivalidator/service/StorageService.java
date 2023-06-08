package ch.so.agi.ilivalidator.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    void init();

    Path[] store(MultipartFile[] files, String jobId);
        
    void store(Path localFile, Path targetPath);
    
    Path load(Path targetPath, Path localDirectory);

    Path load(String filename);

    void delete(String filename);
}
