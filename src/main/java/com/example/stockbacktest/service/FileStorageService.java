package com.example.stockbacktest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {
    
    @Value("${file.upload.cn-dir}")
    private String cnDir;
    
    @Value("${file.upload.us-dir}")
    private String usDir;
    
    @Value("${file.upload.hk-dir}")
    private String hkDir;
    
    public void init() {
        try {
            Files.createDirectories(Path.of(cnDir));
            Files.createDirectories(Path.of(usDir));
            Files.createDirectories(Path.of(hkDir));
        } catch (Exception e) {
            throw new RuntimeException("Could not create upload directories!", e);
        }
    }
    
    public String storeFile(MultipartFile file, String market, String fileName) {
        try {
            String dir = getMarketDirectory(market);
            Path targetLocation = Path.of(dir).resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName, ex);
        }
    }
    
    private String getMarketDirectory(String market) {
        switch (market.toLowerCase()) {
            case "cn": return cnDir;
            case "us": return usDir;
            case "hk": return hkDir;
            default: throw new IllegalArgumentException("Invalid market: " + market);
        }
    }
    
    public Path loadFile(String market, String filename) {
        String dir = getMarketDirectory(market);
        return Path.of(dir).resolve(filename);
    }
    
    public boolean fileExists(String market, String filename) {
        Path file = loadFile(market, filename);
        return Files.exists(file);
    }
}