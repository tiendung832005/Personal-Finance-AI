package com.data.personalfinanceinsightai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.request.account.CreateSharedAccountRequest;
import com.data.personalfinanceinsightai.dto.response.account.AccountResponse;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.FamilyGroup;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.AccountScope;
import com.data.personalfinanceinsightai.entity.enums.AccountType;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.FamilyGroupRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedAccountServiceTest {

    private static final Long GROUP_ID = 10L;
    private static final Long ADMIN_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FamilyGroupRepository familyGroupRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    private SharedAccountServiceImpl sharedAccountService;

    @BeforeEach
    void setUp() {
        sharedAccountService = new SharedAccountServiceImpl(
                userRepository, familyGroupRepository, accountRepository, groupAuthorizationService);
    }

    @Test
    void createSharedAccount_adminSuccess() {
        User admin = User.builder().id(ADMIN_ID).email("admin@test.com").build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(familyGroupRepository.findById(GROUP_ID))
                .thenReturn(Optional.of(FamilyGroup.builder().id(GROUP_ID).name("Gia đình").build()));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> {
                    Account a = invocation.getArgument(0);
                    a.setId(100L);
                    return a;
                });
        when(accountRepository.calculateGroupAccountCurrentBalance(100L))
                .thenReturn(new BigDecimal("10000000"));

        CreateSharedAccountRequest req = new CreateSharedAccountRequest();
        req.setName("Quỹ gia đình");
        req.setType(AccountType.CASH);
        req.setInitialBalance(new BigDecimal("10000000"));
        req.setCurrency("VND");

        AccountResponse response = sharedAccountService.createSharedAccount("admin@test.com", GROUP_ID, req);

        assertThat(response.getGroupId()).isEqualTo(GROUP_ID);
        assertThat(response.getScope()).isEqualTo(AccountScope.SHARED);
        assertThat(response.getCurrentBalance()).isEqualByComparingTo("10000000");

        ArgumentCaptor<Account> cap = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(cap.capture());
        Account saved = cap.getValue();
        assertThat(saved.getFamilyId()).isEqualTo(GROUP_ID);
        assertThat(saved.getScope()).isEqualTo(AccountScope.SHARED);
        assertThat(saved.getBalance()).isEqualByComparingTo("10000000");
        verify(groupAuthorizationService).requireAdmin(GROUP_ID, ADMIN_ID);
    }

    @Test
    void createSharedAccount_memberForbidden() {
        User member = User.builder().id(MEMBER_ID).email("member@test.com").build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        doThrow(new ForbiddenException("Chỉ ADMIN mới có quyền thực hiện thao tác này"))
                .when(groupAuthorizationService)
                .requireAdmin(GROUP_ID, MEMBER_ID);

        CreateSharedAccountRequest req = new CreateSharedAccountRequest();
        req.setName("Quỹ gia đình");
        req.setType(AccountType.CASH);
        req.setInitialBalance(new BigDecimal("10000000"));

        assertThatThrownBy(() -> sharedAccountService.createSharedAccount("member@test.com", GROUP_ID, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createSharedAccount_nonMemberForbidden() {
        User outsider = User.builder().id(99L).email("outsider@test.com").build();
        when(userRepository.findByEmail("outsider@test.com")).thenReturn(Optional.of(outsider));
        doThrow(new ForbiddenException("Bạn không phải thành viên của nhóm này"))
                .when(groupAuthorizationService)
                .requireAdmin(GROUP_ID, 99L);

        CreateSharedAccountRequest req = new CreateSharedAccountRequest();
        req.setName("Quỹ gia đình");
        req.setType(AccountType.CASH);
        req.setInitialBalance(new BigDecimal("10000000"));

        assertThatThrownBy(() -> sharedAccountService.createSharedAccount("outsider@test.com", GROUP_ID, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getSharedAccounts_memberCanView() {
        User member = User.builder().id(MEMBER_ID).email("member@test.com").build();
        Account shared = Account.builder()
                .id(5L)
                .user(member)
                .familyId(GROUP_ID)
                .scope(AccountScope.SHARED)
                .name("Quỹ gia đình")
                .type(AccountType.CASH)
                .balance(new BigDecimal("10000000"))
                .currency("VND")
                .build();

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(accountRepository.findByFamilyIdAndScopeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        GROUP_ID, AccountScope.SHARED))
                .thenReturn(List.of(shared));
        when(accountRepository.calculateGroupAccountCurrentBalance(5L))
                .thenReturn(new BigDecimal("8000000"));

        List<AccountResponse> result = sharedAccountService.getSharedAccounts("member@test.com", GROUP_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScope()).isEqualTo(AccountScope.SHARED);
        assertThat(result.get(0).getCurrentBalance()).isEqualByComparingTo("8000000");
        verify(groupAuthorizationService).requireMember(GROUP_ID, MEMBER_ID);
    }

    @Test
    void getSharedAccounts_nonMemberForbidden() {
        User outsider = User.builder().id(99L).email("outsider@test.com").build();
        when(userRepository.findByEmail("outsider@test.com")).thenReturn(Optional.of(outsider));
        doThrow(new ForbiddenException("Bạn không phải thành viên của nhóm này"))
                .when(groupAuthorizationService)
                .requireMember(GROUP_ID, 99L);

        assertThatThrownBy(() -> sharedAccountService.getSharedAccounts("outsider@test.com", GROUP_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getSharedAccounts_onlySharedScope() {
        User admin = User.builder().id(ADMIN_ID).email("admin@test.com").build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        sharedAccountService.getSharedAccounts("admin@test.com", GROUP_ID);

        verify(accountRepository)
                .findByFamilyIdAndScopeAndDeletedAtIsNullOrderByCreatedAtDesc(
                        eq(GROUP_ID), eq(AccountScope.SHARED));
    }
}
