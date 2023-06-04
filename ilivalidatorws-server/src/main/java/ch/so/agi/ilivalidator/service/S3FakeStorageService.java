package ch.so.agi.ilivalidator.service;

import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

public class S3FakeStorageService implements StorageService {

    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    @Override
    public Path[] store(MultipartFile[] files) {
        // TODO Auto-generated method stub
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
