package ch.so.agi.ilivalidator.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements StorageService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String folderPrefix;

    private final Path workDirectoryPath;

    public LocalFileStorageService(
            @Value("${app.workDirectory}") String workDirectory,
            @Value("${app.folderPrefix}") String folderPrefix) {
        this.workDirectoryPath = Paths.get(workDirectory);
        this.folderPrefix = folderPrefix;
    }
    
    @Override
    public void init() {}

    @Override
    public Path[] store(MultipartFile[] files, String jobId) throws IOException {
        log.debug("Work directory: {}", workDirectoryPath);
        
        Path jobDirectoryPath = workDirectoryPath.resolve(folderPrefix + jobId);
        try {
            Files.createDirectory(jobDirectoryPath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Could not create directory: " + jobDirectoryPath + ". Please try again!");
        } 

        List<Path> paths = new ArrayList<>();
        for (MultipartFile file : files) {
            // Normalize file name
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            
            try {
                // Check if the file's name contains invalid characters
                if(fileName.contains("..")) {
                    throw new LocalFileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
                }
                
                // Copy file to the target location (Replacing existing file with the same name)
                Path targetLocation = jobDirectoryPath.resolve(fileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                paths.add(targetLocation);
            
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Could not store file " + fileName + ". Please try again!");
            }
        }
        return paths.toArray(new Path[0]);
    }
    
    @Override
    public Path load(String filePath) {
        Path path = workDirectoryPath.resolve(filePath);
        return path;
    }


}
