package ch.so.agi.ilivalidator.service;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.carlspring.cloud.storage.s3fs.S3FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class S3StorageService implements StorageService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.workDirectory}")
    private String workDirectory;

    @Value("${app.folderPrefix}")
    private String folderPrefix;

    private S3FileSystem s3fs;
    
    public S3StorageService(FileSystem s3FileSystem) {
        this.s3fs = (S3FileSystem) s3FileSystem;
    }
    
    @Override
    public void init() {
        // TODO Auto-generated method stub
    }

    @Override
    public Path[] store(MultipartFile[] files, String jobId) throws IOException {    
        log.debug("Work directory: {}", workDirectory);
        Path workDirectoryPath = s3fs.getPath(workDirectory);
        log.debug("Work directory Path: " + workDirectoryPath);
        Path jobDirectoryPath = workDirectoryPath.resolve(jobId);
        try {
            Files.createDirectory(jobDirectoryPath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Could not create directory. Please try again!");
        } 
        
        List<Path> paths = new ArrayList<>();
        for (MultipartFile file : files) {
            // Normalize file name
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());           
            try {
                // Check if the file's name contains invalid characters
                if(fileName.contains("..")) {
                    throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
                }
                
                // Copy file to the target location (Replacing existing file with the same name)
                Path targetLocation = jobDirectoryPath.resolve(fileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                paths.add(targetLocation);
            
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Could not store file. Please try again!");
            } 
        }
        return paths.toArray(new Path[0]);
    }

    /**
     * @param localFile local file
     * @param targetPath Target path where the file will be written. Relative to the work directory.
     */
    @Override
    public void store(Path localFile, Path targetPath) {
        Path workDirectoryPath = s3fs.getPath(workDirectory);
        targetPath = workDirectoryPath.resolve(targetPath.toString());
        try {
            Files.copy(localFile, targetPath);
        } catch (IOException e) {
            throw new FileStorageException(e.getMessage());
        }
    }
    
    @Override
    public Path load(Path targetPath, Path localDirectory) {
        Path localCopy = localDirectory.resolve(Paths.get(targetPath.getFileName().toString()));
        try {
            Files.copy(targetPath, localCopy, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException(e.getMessage());
        }
        return localCopy;
    }

    @Override
    public Path load(String filePath) {
        Path workDirectoryPath = s3fs.getPath(workDirectory);
        Path path = workDirectoryPath.resolve(filePath);
        return path;
    }

    @Override
    public void delete(String filename) {
        // TODO Auto-generated method stub
    }
}
