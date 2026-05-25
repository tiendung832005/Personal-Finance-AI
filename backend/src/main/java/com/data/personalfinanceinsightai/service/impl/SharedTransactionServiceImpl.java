package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.transaction.TransactionCreateRequest;
import com.data.personalfinanceinsightai.dto.response.PagedResponse;
import com.data.personalfinanceinsightai.dto.response.transaction.CategorizationResult;
import com.data.personalfinanceinsightai.dto.response.transaction.SharedTransactionResponse;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.Transaction;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.AccountScope;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.entity.enums.TransactionScope;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.exception.InvalidTransactionException;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.CategorizationService;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import com.data.personalfinanceinsightai.service.SharedTransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SharedTransactionServiceImpl implements SharedTransactionService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final CategorizationService categorizationService;

    @Override
    @Transactional
    public SharedTransactionResponse createSharedTransaction(
            String email, Long groupId, TransactionCreateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());

        LocalDate today = LocalDate.now(APP_ZONE);
        validateAmount(request.getAmount());
        validateTransactionDate(request.getTransactionDate(), today);

        Account account = accountRepository
                .findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));
        validateSharedAccount(account, groupId);

        Long finalCategoryId = request.getCategoryId();
        boolean autoCategorized = Boolean.TRUE.equals(request.getIsAutoCategorized());

        // Auto-categorize nếu không có categoryId (T13 - Sprint 6)
        if (finalCategoryId == null && request.getDescription() != null) {
            try {
                CategorizationResult result = categorizationService
                        .categorize(request.getDescription(), user.getId());
                if (result.isSuccessful()) {
                    finalCategoryId = result.getCategoryId();
                    autoCategorized = true;
                }
            } catch (Exception e) {
                // AI lỗi -> shared transaction vẫn tạo được, không crash
                log.warn("Auto-categorize failed for shared transaction, creating without category: {}",
                        e.getMessage());
            }
        }

        Category category = null;
        if (finalCategoryId != null) {
            category = categoryRepository
                    .findVisibleByIdAndUserId(finalCategoryId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            validateCategoryMatchesTransactionType(category, request.getType());
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .familyId(groupId)
                .scope(TransactionScope.SHARED)
                .amount(request.getAmount())
                .type(request.getType())
                .description(trimToNull(request.getDescription()))
                .transactionDate(request.getTransactionDate())
                .autoCategorized(autoCategorized)
                .flaggedAnomaly(false)
                .note(trimToNull(request.getNote()))
                .deletedAt(null)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return SharedTransactionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SharedTransactionResponse> getSharedTransactions(
            String email,
            Long groupId,
            String month,
            Long categoryId,
            TransactionType type,
            int page,
            int size) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());

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

        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        long total = transactionRepository.countSharedTransactionsWithFilters(
                groupId, categoryId, type, monthStart, monthEnd);

        List<SharedTransactionResponse> content = transactionRepository
                .findSharedTransactionsWithFilters(groupId, categoryId, type, monthStart, monthEnd, pageable)
                .stream()
                .map(SharedTransactionResponse::fromEntity)
                .toList();

        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        return PagedResponse.<SharedTransactionResponse>builder()
                .content(content)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    @Override
    @Transactional
    public void deleteSharedTransaction(String email, Long groupId, Long transactionId) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());

        Transaction transaction = transactionRepository
                .findByIdAndFamilyIdAndScopeAndDeletedAtIsNull(
                        transactionId, groupId, TransactionScope.SHARED)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch không tồn tại"));

        boolean isCreator = transaction.getUser().getId().equals(user.getId());
        boolean isAdmin = groupAuthorizationService.isAdmin(groupId, user.getId());

        if (!isCreator && !isAdmin) {
            throw new ForbiddenException("Chỉ người tạo giao dịch hoặc ADMIN mới có thể xóa");
        }

        transaction.setDeletedAt(LocalDateTime.now(APP_ZONE));
        transactionRepository.save(transaction);
    }

    private void validateSharedAccount(Account account, Long groupId) {
        if (account.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Tài khoản không tồn tại");
        }
        if (account.getScope() != AccountScope.SHARED) {
            throw new InvalidTransactionException(
                    "Tài khoản này không thuộc nhóm. Chỉ được dùng tài khoản chung của nhóm.");
        }
        if (account.getFamilyId() == null || !account.getFamilyId().equals(groupId)) {
            throw new InvalidTransactionException(
                    "Tài khoản này không thuộc nhóm. Chỉ được dùng tài khoản chung của nhóm.");
        }
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
