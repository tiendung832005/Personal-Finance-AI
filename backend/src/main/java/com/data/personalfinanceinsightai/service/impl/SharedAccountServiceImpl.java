package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.account.CreateSharedAccountRequest;
import com.data.personalfinanceinsightai.dto.response.account.AccountResponse;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.AccountScope;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.FamilyGroupRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import com.data.personalfinanceinsightai.service.SharedAccountService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SharedAccountServiceImpl implements SharedAccountService {

    private final UserRepository userRepository;
    private final FamilyGroupRepository familyGroupRepository;
    private final AccountRepository accountRepository;
    private final GroupAuthorizationService groupAuthorizationService;

    @Override
    @Transactional
    public AccountResponse createSharedAccount(String email, Long groupId, CreateSharedAccountRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireAdmin(groupId, user.getId());
        familyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhóm không tồn tại"));

        String currency = (request.getCurrency() == null || request.getCurrency().isBlank())
                ? "VND"
                : request.getCurrency().trim().toUpperCase();

        BigDecimal initialBalance = request.getInitialBalance().stripTrailingZeros();

        Account account = Account.builder()
                .user(user)
                .familyId(groupId)
                .scope(AccountScope.SHARED)
                .name(request.getName().trim())
                .type(request.getType())
                .balance(initialBalance)
                .currency(currency)
                .defaultAccount(false)
                .build();

        Account saved = accountRepository.save(account);
        BigDecimal currentBalance = resolveCurrentBalance(saved.getId());
        return AccountResponse.fromEntityWithBalance(saved, currentBalance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getSharedAccounts(String email, Long groupId) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        groupAuthorizationService.requireMember(groupId, user.getId());

        return accountRepository
                .findByFamilyIdAndScopeAndDeletedAtIsNullOrderByCreatedAtDesc(groupId, AccountScope.SHARED)
                .stream()
                .map(account -> AccountResponse.fromEntityWithBalance(
                        account, resolveCurrentBalance(account.getId())))
                .toList();
    }

    private BigDecimal resolveCurrentBalance(Long accountId) {
        BigDecimal balance = accountRepository.calculateGroupAccountCurrentBalance(accountId);
        return balance != null ? balance : BigDecimal.ZERO;
    }
}
