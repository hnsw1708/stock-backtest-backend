package com.example.stockbacktest.controller;

import com.example.stockbacktest.service.DataParseService;
import com.example.stockbacktest.service.FileStorageService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/api")
public class ChartDataController {
    
    @Autowired
    private DataParseService dataParseService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @GetMapping("/chart-data/{market}")
    public String getChartData(@PathVariable String market) {
        JsonObject response = new JsonObject();
        JsonArray indicesData = new JsonArray();
        
        switch (market.toLowerCase()) {
            case "cn":
                addIndexData(indicesData, market, "000001.SH.csv", "上证指数");
                addIndexData(indicesData, market, "000300.SH.csv", "沪深300");
                addIndexData(indicesData, market, "399006.SZ.csv", "创业板指");
                break;
            case "us":
                addIndexData(indicesData, market, "DJI.csv", "道琼斯");
                addIndexData(indicesData, market, "NASDAQ.csv", "纳斯达克");
                addIndexData(indicesData, market, "SP500.csv", "标普500");
                break;
            case "hk":
                addIndexData(indicesData, market, "HSI.csv", "恒生指数");
                addIndexData(indicesData, market, "HKAH.csv", "AH联动指数");
                break;
        }
        
        response.add("indices", indicesData);
        JsonArray moneyGrowData = getMoneyGrowData(market);
        response.add("moneyGrow", moneyGrowData);
        
        return response.toString();
    }
    
    private void addIndexData(JsonArray indicesData, String market, String filename, String name) {
        if (fileStorageService.fileExists(market, filename)) {
            JsonObject indexData = new JsonObject();
            indexData.addProperty("name", name);
            
            JsonArray dataArray = new JsonArray();
            dataParseService.parseStockData(market, filename).forEach(stockData -> {
                JsonArray item = new JsonArray();
                item.add(stockData.getDate());
                item.add(stockData.getOpen());
                item.add(stockData.getClose());
                item.add(stockData.getLow());
                item.add(stockData.getHigh());
                item.add(stockData.getVolume());
                dataArray.add(item);
            });
            
            indexData.add("data", dataArray);
            indicesData.add(indexData);
        }
    }
    
    private JsonArray getMoneyGrowData(String market) {
        JsonArray moneyGrowArray = new JsonArray();
        String filename = "MoneyGrow_" + market + ".txt";
        
        if (fileStorageService.fileExists(market, filename)) {
            dataParseService.parseMoneyGrowData(market, filename).forEach(moneyData -> {
                JsonArray item = new JsonArray();
                item.add(moneyData.getDate());
                item.add(moneyData.getValue());
                moneyGrowArray.add(item);
            });
        }
        
        return moneyGrowArray;
    }
}
