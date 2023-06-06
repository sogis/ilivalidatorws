package ch.so.agi.ilivalidator.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

//import org.carlspring.cloud.storage.s3fs.S3FileSystemProvider;
//import org.carlspring.cloud.storage.s3fs.S3Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


public class S3FakeStorageService implements StorageService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    
//    public S3FakeStorageService(FileSystem s3FileSystem) {
//    }
    
    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    @Override
    public Path[] store(MultipartFile[] files) {        
        return null;
    }

    @Override
    public Path store(MultipartFile file) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Path load(String filename) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(String filename) {
        // TODO Auto-generated method stub

    }

}
