package com.example.stockbacktest.service;

import com.example.stockbacktest.model.StockData;
import com.example.stockbacktest.model.MoneyGrowData;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DataParseService {

    @Autowired
    private FileStorageService fileStorageService;

    // 日期格式化器
    private static final DateTimeFormatter STOCK_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONEY_GROW_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<StockData> parseStockData(String market, String filename) {
        List<StockData> stockDataList = new ArrayList<>();
        try {
            Path filePath = fileStorageService.loadFile(market, filename);
            List<String> lines = FileUtils.readLines(filePath.toFile(), "UTF-8");

            System.out.println("解析股票数据文件: " + filename);
            System.out.println("文件路径: " + filePath);
            System.out.println("总行数: " + lines.size());

            int successCount = 0;
            int errorCount = 0;

            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("date") || line.startsWith("日期")) {
                    continue;
                }

                String[] parts = line.split(",");

                if (parts.length >= 6) {
                    try {
                        StockData data = new StockData(
                                parts[0].trim(),
                                Double.parseDouble(parts[1].trim()),
                                Double.parseDouble(parts[2].trim()),
                                Double.parseDouble(parts[3].trim()),
                                Double.parseDouble(parts[4].trim()),
                                Double.parseDouble(parts[5].trim())
                        );
                        stockDataList.add(data);
                        successCount++;

                    } catch (NumberFormatException e) {
                        System.err.println("股票数据数值格式错误 - 行: " + line + ", 错误: " + e.getMessage());
                        errorCount++;
                    }
                } else {
                    System.err.println("股票数据列数不足 - 行: " + line + ", 实际列数: " + parts.length);
                    errorCount++;
                }
            }

            // 按日期升序排序（从早到晚）
            stockDataList.sort(Comparator.comparing(data -> {
                try {
                    return LocalDate.parse(data.getDate(), STOCK_DATE_FORMATTER);
                } catch (Exception e) {
                    System.err.println("股票数据日期解析失败: " + data.getDate());
                    return LocalDate.MIN;
                }
            }));

            System.out.println("股票数据解析完成: 成功=" + successCount + ", 失败=" + errorCount +
                    ", 总计=" + stockDataList.size());

        } catch (IOException e) {
            System.err.println("股票数据文件读取失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("股票数据解析过程发生未知错误: " + e.getMessage());
            e.printStackTrace();
        }
        return stockDataList;
    }

    public List<MoneyGrowData> parseMoneyGrowData(String market, String filename, List<StockData> stockDataList) {
        List<MoneyGrowData> moneyGrowList = new ArrayList<>();
        try {
            Path filePath = fileStorageService.loadFile(market, filename);
            List<String> lines = FileUtils.readLines(filePath.toFile(), "UTF-8");

            System.out.println("解析资金曲线文件: " + filename);
            System.out.println("文件路径: " + filePath);
            System.out.println("总行数: " + lines.size());

            // 第一步：解析原始资金曲线数据
            Map<LocalDate, Double> rawMoneyData = new HashMap<>();
            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("date") || line.startsWith("日期")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    try {
                        String dateStr = parts[0].trim();
                        double moneyValue = Double.parseDouble(parts[1].trim());

                        // 将资金曲线日期格式从 yyyyMMdd 转换为 LocalDate
                        LocalDate moneyDate = LocalDate.parse(dateStr, MONEY_GROW_DATE_FORMATTER);
                        rawMoneyData.put(moneyDate, moneyValue);

                    } catch (Exception e) {
                        System.err.println("资金数据解析错误 - 行: " + line + ", 错误: " + e.getMessage());
                    }
                }
            }

            // 第二步：与股票数据日期对齐
            if (stockDataList != null && !stockDataList.isEmpty()) {
                System.out.println("开始资金曲线与股票数据日期对齐...");

                // 获取股票数据的日期范围
                LocalDate firstStockDate = LocalDate.parse(stockDataList.getFirst().getDate(), STOCK_DATE_FORMATTER);
                LocalDate lastStockDate = LocalDate.parse(
                        stockDataList.getLast().getDate(), STOCK_DATE_FORMATTER);

                System.out.println("股票数据日期范围: " + firstStockDate + " 到 " + lastStockDate);

                // 获取资金曲线的日期范围
                LocalDate firstMoneyDate = rawMoneyData.keySet().stream().min(LocalDate::compareTo).orElse(null);
                LocalDate lastMoneyDate = rawMoneyData.keySet().stream().max(LocalDate::compareTo).orElse(null);

                if (firstMoneyDate != null && lastMoneyDate != null) {
                    System.out.println("资金曲线原始日期范围: " + firstMoneyDate + " 到 " + lastMoneyDate);
                }

                // 为每个股票数据日期找到对应的资金值
                Double lastValidMoneyValue = null;
                for (StockData stockData : stockDataList) {
                    LocalDate stockDate = LocalDate.parse(stockData.getDate(), STOCK_DATE_FORMATTER);

                    // 查找该日期的资金值
                    Double moneyValue = rawMoneyData.get(stockDate);

                    // 如果该日期没有资金数据，使用前一个有效值（向前填充）
                    if (moneyValue == null) {
                        moneyValue = lastValidMoneyValue;
                    } else {
                        lastValidMoneyValue = moneyValue;
                    }

                    // 如果仍然没有有效值，跳过或使用默认值
                    if (moneyValue != null) {
                        // 将股票日期格式转换回字符串用于资金曲线数据
                        String formattedDate = stockDate.format(STOCK_DATE_FORMATTER);
                        MoneyGrowData alignedData = new MoneyGrowData(formattedDate, moneyValue);
                        moneyGrowList.add(alignedData);
                    }
                }

                System.out.println("日期对齐完成: 原始资金数据 " + rawMoneyData.size() + " 条, 对齐后 " + moneyGrowList.size() + " 条");

            } else {
                // 如果没有股票数据，使用原始资金数据（已排序）
                rawMoneyData.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            String formattedDate = entry.getKey().format(STOCK_DATE_FORMATTER);
                            moneyGrowList.add(new MoneyGrowData(formattedDate, entry.getValue()));
                        });
            }

        } catch (IOException e) {
            System.err.println("资金曲线文件读取失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("资金曲线解析过程发生未知错误: " + e.getMessage());
            e.printStackTrace();
        }
        return moneyGrowList;
    }

    // 为了向后兼容，保留原来的方法签名
    public List<MoneyGrowData> parseMoneyGrowData(String market, String filename) {
        return parseMoneyGrowData(market, filename, null);
    }
}