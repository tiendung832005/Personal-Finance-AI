package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.transaction.TransactionCreateRequest;
import com.data.personalfinanceinsightai.dto.request.transaction.TransactionUpdateRequest;
import com.data.personalfinanceinsightai.dto.response.transaction.TransactionResponse;
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
import com.data.personalfinanceinsightai.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public TransactionResponse create(String email, TransactionCreateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDate today = LocalDate.now(APP_ZONE);
        validateAmount(request.getAmount());
        validateTransactionDate(request.getTransactionDate(), today);

        Account account = accountRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(request.getAccountId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        validateFamilyScope(user, account, request.getFamilyId());

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository
                    .findVisibleByIdAndUserId(request.getCategoryId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            validateCategoryMatchesTransactionType(category, request.getType());
        }

        applyAccountBalanceChange(account, request.getType(), request.getAmount());
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .familyId(null)
                .scope(TransactionScope.PERSONAL)
                .amount(request.getAmount())
                .type(request.getType())
                .description(trimToNull(request.getDescription()))
                .transactionDate(request.getTransactionDate())
                .autoCategorized(false)
                .flaggedAnomaly(false)
                .note(trimToNull(request.getNote()))
                .deletedAt(null)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> listForUser(String email, String month, Long categoryId, TransactionType type) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDate monthStart = null;
        LocalDate monthEnd = null;
        if (month != null && !month.isBlank()) {
            try {
                YearMonth ym = YearMonth.parse(month.trim());
                monthStart = ym.atDay(1);
                monthEnd = ym.plusMonths(1).atDay(1);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("month must be in format yyyy-MM");
            }
        }

        return transactionRepository
                .findForUserWithFilters(user.getId(), categoryId, type, monthStart, monthEnd)
                .stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getById(String email, Long id) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Transaction transaction = transactionRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return TransactionResponse.fromEntity(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse update(String email, Long id, TransactionUpdateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Transaction transaction = transactionRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        LocalDate today = LocalDate.now(APP_ZONE);
        Account account = transaction.getAccount();

        if (request.getAmount() != null) {
            validateAmount(request.getAmount());
        }
        if (request.getTransactionDate() != null) {
            validateTransactionDate(request.getTransactionDate(), today);
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository
                    .findVisibleByIdAndUserId(request.getCategoryId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            validateCategoryMatchesTransactionType(category, transaction.getType());
            transaction.setCategory(category);
        }

        if (request.getAmount() != null
                && request.getAmount().compareTo(transaction.getAmount()) != 0) {
            revertAccountBalanceChange(account, transaction.getType(), transaction.getAmount());
            applyAccountBalanceChange(account, transaction.getType(), request.getAmount());
            accountRepository.save(account);
            transaction.setAmount(request.getAmount());
        }

        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }

        if (request.getDescription() != null) {
            transaction.setDescription(trimToNull(request.getDescription()));
        }

        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void softDelete(String email, Long id) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Transaction transaction = transactionRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        Account account = transaction.getAccount();
        revertAccountBalanceChange(account, transaction.getType(), transaction.getAmount());
        accountRepository.save(account);

        transaction.setDeletedAt(LocalDateTime.now(APP_ZONE));
        transactionRepository.save(transaction);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount must have at most 2 decimal places");
        }
    }

    private void validateTransactionDate(LocalDate transactionDate, LocalDate today) {
        if (transactionDate.isAfter(today)) {
            throw new IllegalArgumentException("transactionDate cannot be in the future");
        }
    }

    private void validateFamilyScope(User user, Account account, Long requestFamilyId) {
        if (requestFamilyId == null) {
            return;
        }
        if (user.getFamilyId() == null || !user.getFamilyId().equals(requestFamilyId)) {
            throw new IllegalArgumentException("familyId is not allowed for this user");
        }
        if (account.getFamilyId() == null || !account.getFamilyId().equals(requestFamilyId)) {
            throw new IllegalArgumentException("Account does not belong to this family");
        }
    }

    private void validateCategoryMatchesTransactionType(Category category, TransactionType transactionType) {
        CategoryType categoryType = category.getType();
        if (categoryType == CategoryType.BOTH) {
            return;
        }
        if (categoryType == CategoryType.EXPENSE && transactionType != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Category type does not match transaction type");
        }
        if (categoryType == CategoryType.INCOME && transactionType != TransactionType.INCOME) {
            throw new IllegalArgumentException("Category type does not match transaction type");
        }
    }

    private void applyAccountBalanceChange(Account account, TransactionType type, BigDecimal amount) {
        BigDecimal current = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        BigDecimal updated = switch (type) {
            case INCOME -> current.add(amount);
            case EXPENSE -> current.subtract(amount);
        };
        account.setBalance(updated);
    }

    private void revertAccountBalanceChange(Account account, TransactionType type, BigDecimal amount) {
        BigDecimal current = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        BigDecimal updated = switch (type) {
            case INCOME -> current.subtract(amount);
            case EXPENSE -> current.add(amount);
        };
        account.setBalance(updated);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
