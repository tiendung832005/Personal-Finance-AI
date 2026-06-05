package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.transaction.ConfirmImportRequest;
import com.data.personalfinanceinsightai.dto.response.transaction.CategorizationResult;
import com.data.personalfinanceinsightai.dto.response.transaction.imports.ImportConfirmResponse;
import com.data.personalfinanceinsightai.dto.response.transaction.imports.ImportErrorRow;
import com.data.personalfinanceinsightai.dto.response.transaction.imports.ImportPreviewResponse;
import com.data.personalfinanceinsightai.dto.response.transaction.imports.ImportPreviewRow;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.Transaction;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.entity.enums.TransactionScope;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImportService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final int MAX_ROWS = 500;

    private final ImportCSVParser importCSVParser;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategorizationService categorizationService;

    public ImportPreviewResponse previewImport(String email, MultipartFile file) {
        validateFile(file);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ImportCSVParser.RawCsvRow> rawRows = importCSVParser.parse(file);
        if (rawRows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("File CSV t?i da 500 dòng d? li?u");
        }

        List<ImportPreviewRow> validRows = new ArrayList<>();
        List<ImportErrorRow> errorRows = new ArrayList<>();

        for (ImportCSVParser.RawCsvRow rawRow : rawRows) {
            try {
                LocalDate date = parseDate(rawRow.getDate());
                BigDecimal amount = parseAmount(rawRow.getAmount());
                TransactionType type = parseType(rawRow.getType());
                String description = trimToNull(rawRow.getDescription());

                if (description != null && description.length() > 500) {
                    throw new IllegalArgumentException("Mô t? t?i da 500 ký t?");
                }

                CategorizationResult result = categorizationService.categorize(description, user.getId());

                validRows.add(ImportPreviewRow.builder()
                        .rowNumber(rawRow.getRowNumber())
                        .date(date)
                        .amount(amount)
                        .type(type)
                        .description(description)
                        .note(trimToNull(rawRow.getNote()))
                        .categoryId(result.getCategoryId())
                        .categoryName(result.getSuggestedCategoryName())
                        .aiSource(result.getSource())
                        .autoCategorized(result.isSuccessful())
                        .build());
            } catch (Exception ex) {
                errorRows.add(ImportErrorRow.builder()
                        .rowNumber(rawRow.getRowNumber())
                        .rawData(rawRow.getRawData())
                        .error(ex.getMessage())
                        .build());
            }
        }

        return ImportPreviewResponse.builder()
                .validCount(validRows.size())
                .errorCount(errorRows.size())
                .validRows(validRows)
                .errorRows(errorRows)
                .build();
    }

    @Transactional
    public ImportConfirmResponse confirmImport(String email, ConfirmImportRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(request.getAccountId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm th?y tài kho?n"));

        List<Transaction> entities = new ArrayList<>();

        for (ConfirmImportRequest.ConfirmImportRow row : request.getValidRows()) {
            Category category = categoryRepository
                    .findVisibleByIdAndUserId(row.getCategoryId(), user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Row " + row.getRowNumber() + ": Danh m?c không h?p l?"));

            validateCategoryMatchesTransactionType(category, row.getType(), row.getRowNumber());
            validateConfirmRow(row);

            applyAccountBalanceChange(account, row.getType(), row.getAmount());

            entities.add(Transaction.builder()
                    .user(user)
                    .account(account)
                    .category(category)
                    .scope(TransactionScope.PERSONAL)
                    .amount(row.getAmount())
                    .type(row.getType())
                    .description(trimToNull(row.getDescription()))
                    .transactionDate(row.getDate())
                    .autoCategorized(Boolean.TRUE.equals(row.getAutoCategorized()))
                    .flaggedAnomaly(false)
                    .note(trimToNull(row.getNote()))
                    .build());
        }

        accountRepository.save(account);
        transactionRepository.saveAll(entities);
        return new ImportConfirmResponse(entities.size());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng ch?n file CSV");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Ch? ch?p nh?n file .csv");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File t?i da 5MB");
        }
    }

    private LocalDate parseDate(String value) {
        try {
            LocalDate date = LocalDate.parse(value);
            if (date.isAfter(LocalDate.now(APP_ZONE))) {
                throw new IllegalArgumentException("Ngày giao d?ch không du?c ? tuong lai");
            }
            return date;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Ngày ph?i dúng d?nh d?ng yyyy-MM-dd");
        }
    }

    private BigDecimal parseAmount(String value) {
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("S? ti?n ph?i l?n hon 0");
            }
            if (amount.precision() > 15) {
                throw new IllegalArgumentException("S? ti?n t?i da 15 ch? s?");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("S? ti?n không h?p l?");
        }
    }

    private TransactionType parseType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Lo?i giao d?ch b?t bu?c là INCOME ho?c EXPENSE");
        }
        try {
            TransactionType type = TransactionType.valueOf(value.trim().toUpperCase());
            if (type != TransactionType.INCOME && type != TransactionType.EXPENSE) {
                throw new IllegalArgumentException("Lo?i giao d?ch ch? ch?p nh?n INCOME ho?c EXPENSE");
            }
            return type;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Lo?i giao d?ch ch? ch?p nh?n INCOME ho?c EXPENSE");
        }
    }

    private void validateConfirmRow(ConfirmImportRequest.ConfirmImportRow row) {
        if (row.getDate().isAfter(LocalDate.now(APP_ZONE))) {
            throw new IllegalArgumentException("Row " + row.getRowNumber() + ": Ngày không du?c ? tuong lai");
        }
        if (row.getAmount().compareTo(BigDecimal.ZERO) <= 0 || row.getAmount().precision() > 15) {
            throw new IllegalArgumentException("Row " + row.getRowNumber() + ": S? ti?n không h?p l?");
        }
        String description = trimToNull(row.getDescription());
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("Row " + row.getRowNumber() + ": Mô t? t?i da 500 ký t?");
        }
    }

    private void validateCategoryMatchesTransactionType(Category category, TransactionType type, int rowNumber) {
        CategoryType categoryType = category.getType();
        if (categoryType == CategoryType.BOTH) {
            return;
        }
        if (categoryType == CategoryType.EXPENSE && type != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Row " + rowNumber + ": Danh m?c không kh?p lo?i giao d?ch");
        }
        if (categoryType == CategoryType.INCOME && type != TransactionType.INCOME) {
            throw new IllegalArgumentException("Row " + rowNumber + ": Danh m?c không kh?p lo?i giao d?ch");
        }
    }

    private void applyAccountBalanceChange(Account account, TransactionType type, BigDecimal amount) {
        BigDecimal current = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        account.setBalance(type == TransactionType.INCOME ? current.add(amount) : current.subtract(amount));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


