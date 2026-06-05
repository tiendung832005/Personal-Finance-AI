package com.data.personalfinanceinsightai.service;

import com.opencsv.CSVReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImportCSVParser {

    private static final String[] REQUIRED_HEADERS = {"date", "amount", "type", "description", "note"};

    public List<RawCsvRow> parse(MultipartFile file) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("File CSV tr?ng");
            }

            String[] header = rows.get(0);
            validateHeader(header);

            List<RawCsvRow> result = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                result.add(new RawCsvRow(
                        i + 1,
                        cell(row, 0),
                        cell(row, 1),
                        cell(row, 2),
                        cell(row, 3),
                        cell(row, 4),
                        String.join(",", row)
                ));
            }
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không th? d?c file CSV: " + e.getMessage());
        }
    }

    private void validateHeader(String[] header) {
        if (header.length < REQUIRED_HEADERS.length) {
            throw new IllegalArgumentException("Header CSV không dúng d?nh d?ng chu?n");
        }
        for (int i = 0; i < REQUIRED_HEADERS.length; i++) {
            String actual = header[i] == null ? "" : header[i].trim().toLowerCase();
            if (!REQUIRED_HEADERS[i].equals(actual)) {
                throw new IllegalArgumentException("Header CSV ph?i là: date,amount,type,description,note");
            }
        }
    }

    private String cell(String[] row, int idx) {
        if (idx >= row.length || row[idx] == null) {
            return null;
        }
        String val = row[idx].trim();
        return val.isEmpty() ? null : val;
    }

    @Getter
    @AllArgsConstructor
    public static class RawCsvRow {
        private Integer rowNumber;
        private String date;
        private String amount;
        private String type;
        private String description;
        private String note;
        private String rawData;
    }
}

