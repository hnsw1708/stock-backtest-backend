package com.example.stockbacktest.controller;

import com.example.stockbacktest.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Controller
public class FileUploadController {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @PostMapping("/upload/{market}/{fileType}")
    public ResponseEntity<Map<String, String>> uploadFile(
            @PathVariable String market,
            @PathVariable String fileType,
            @RequestParam("file") MultipartFile file) {
        
        try {
            String filename = generateFilename(market, fileType);
            fileStorageService.storeFile(file, market, filename);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "File uploaded successfully: " + filename);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "File upload failed: " + e.getMessage());
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    private String generateFilename(String market, String fileType) {
        return switch (fileType) {
            case "sh000001" -> "000001.SH.csv";
            case "sh000300" -> "000300.SH.csv";
            case "sz399006" -> "399006.SZ.csv";
            case "dji" -> "DJI.csv";
            case "nasdaq" -> "NASDAQ.csv";
            case "sp500" -> "SP500.csv";
            case "hsi" -> "HSI.csv";
            case "hkah" -> "HKAH.csv";
            case "moneygrow" -> "MoneyGrow_" + market + ".txt";
            default -> fileType + ".csv";
        };
    }
}