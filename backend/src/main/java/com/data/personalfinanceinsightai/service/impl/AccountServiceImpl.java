package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.account.AccountCreateRequest;
import com.data.personalfinanceinsightai.dto.response.account.AccountResponse;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.AccountService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> listForUser(String email) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return accountRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
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

        Account account = Account.builder()
                .user(user)
                .familyId(request.getFamilyId())
                .name(request.getName().trim())
                .type(request.getType())
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .defaultAccount(markDefault)
                .build();

        Account saved = accountRepository.save(account);
        return AccountResponse.fromEntity(saved);
    }
}
