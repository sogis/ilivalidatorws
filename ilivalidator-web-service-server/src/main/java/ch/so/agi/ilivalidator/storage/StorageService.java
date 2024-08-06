package ch.so.agi.ilivalidator.storage;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    void init();

    Path[] store(MultipartFile[] files, String jobId) throws IOException;

    Path load(String filePath);
}
