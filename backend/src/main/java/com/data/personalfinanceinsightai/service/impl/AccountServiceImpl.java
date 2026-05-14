package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.account.AccountCreateRequest;
import com.data.personalfinanceinsightai.dto.request.account.AccountUpdateRequest;
import com.data.personalfinanceinsightai.dto.response.account.AccountResponse;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.AccountService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> listForUser(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return accountRepository.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId()).stream()
                .map(AccountResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public AccountResponse create(String email, AccountCreateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFamilyId() != null) {
            if (user.getFamilyId() == null || !user.getFamilyId().equals(request.getFamilyId())) {
                throw new IllegalArgumentException("familyId is not allowed for this user");
            }
        }

        boolean markDefault = Boolean.TRUE.equals(request.getDefaultAccount());
        if (markDefault) {
            accountRepository.clearDefaultAccountsForUser(user.getId());
        }

        String currency = (request.getCurrency() == null || request.getCurrency().isBlank())
                ? "VND"
                : request.getCurrency().trim().toUpperCase();

        BigDecimal openingBalance =
                request.getBalance() == null ? BigDecimal.ZERO : request.getBalance().stripTrailingZeros();

        Account account = Account.builder()
                .user(user)
                .familyId(request.getFamilyId())
                .name(request.getName().trim())
                .type(request.getType())
                .balance(openingBalance)
                .currency(currency)
                .defaultAccount(markDefault)
                .build();

        Account saved = accountRepository.save(account);
        return AccountResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getById(String email, Long id) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Account account = accountRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional
    public AccountResponse update(String email, Long id, AccountUpdateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Account account = accountRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        String currency = request.getCurrency().trim().toUpperCase();
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }

        account.setName(request.getName().trim());
        account.setType(request.getType());
        account.setCurrency(currency);
        return AccountResponse.fromEntity(accountRepository.save(account));
    }

    @Override
    @Transactional
    public String deleteAccount(String email, Long id) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Account account = accountRepository
                .findByIdAndUser_IdAndDeletedAtIsNull(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        long transactionRows = transactionRepository.countByAccount_Id(id);
        if (transactionRows > 0) {
            account.setDeletedAt(LocalDateTime.now());
            account.setDefaultAccount(false);
            accountRepository.save(account);
            return "soft";
        }

        accountRepository.delete(account);
        return "hard";
    }
}
