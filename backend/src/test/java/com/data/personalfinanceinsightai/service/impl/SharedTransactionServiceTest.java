package com.data.personalfinanceinsightai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.personalfinanceinsightai.dto.request.transaction.TransactionCreateRequest;
import com.data.personalfinanceinsightai.dto.response.PagedResponse;
import com.data.personalfinanceinsightai.dto.response.transaction.SharedTransactionResponse;
import com.data.personalfinanceinsightai.entity.Account;
import com.data.personalfinanceinsightai.entity.Transaction;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.AccountScope;
import com.data.personalfinanceinsightai.entity.enums.AccountType;
import com.data.personalfinanceinsightai.entity.enums.TransactionScope;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import com.data.personalfinanceinsightai.exception.ForbiddenException;
import com.data.personalfinanceinsightai.exception.InvalidTransactionException;
import com.data.personalfinanceinsightai.repository.AccountRepository;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.TransactionRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.GroupAuthorizationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SharedTransactionServiceTest {

    private static final Long GROUP_ID = 10L;
    private static final Long OTHER_GROUP_ID = 99L;
    private static final Long ADMIN_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long OUTSIDER_ID = 3L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    private SharedTransactionServiceImpl sharedTransactionService;

    @BeforeEach
    void setUp() {
        sharedTransactionService = new SharedTransactionServiceImpl(
                userRepository,
                accountRepository,
                categoryRepository,
                transactionRepository,
                groupAuthorizationService);
    }

    @Test
    void createSharedTxn_memberSuccess() {
        User member = user(2L, "member@test.com", "Nguyen Van B");
        Account sharedAccount = sharedAccount(5L, GROUP_ID, member);

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(accountRepository.findById(5L)).thenReturn(Optional.of(sharedAccount));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction t = invocation.getArgument(0);
                    t.setId(100L);
                    return t;
                });

        TransactionCreateRequest req = createRequest(5L, new BigDecimal("200000"), "Mua rau");

        SharedTransactionResponse response =
                sharedTransactionService.createSharedTransaction("member@test.com", GROUP_ID, req);

        assertThat(response.getAmount()).isEqualByComparingTo("200000");
        assertThat(response.getScope()).isEqualTo(TransactionScope.SHARED);
        assertThat(response.getCreatedByName()).isEqualTo("Nguyen Van B");
        assertThat(response.getCreatedByUserId()).isEqualTo(2L);

        ArgumentCaptor<Transaction> cap = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(cap.capture());
        assertThat(cap.getValue().getFamilyId()).isEqualTo(GROUP_ID);
        assertThat(cap.getValue().getScope()).isEqualTo(TransactionScope.SHARED);
        verify(groupAuthorizationService).requireMember(GROUP_ID, MEMBER_ID);
    }

    @Test
    void createSharedTxn_nonMemberForbidden() {
        User outsider = user(OUTSIDER_ID, "outsider@test.com", "User C");
        when(userRepository.findByEmail("outsider@test.com")).thenReturn(Optional.of(outsider));
        doThrow(new ForbiddenException("Bạn không phải thành viên của nhóm này"))
                .when(groupAuthorizationService)
                .requireMember(GROUP_ID, OUTSIDER_ID);

        assertThatThrownBy(() -> sharedTransactionService.createSharedTransaction(
                        "outsider@test.com", GROUP_ID, createRequest(5L, new BigDecimal("100000"), "x")))
                .isInstanceOf(ForbiddenException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createSharedTxn_personalAccountRejected() {
        User member = user(MEMBER_ID, "member@test.com", "Member");
        Account personalAccount = Account.builder()
                .id(7L)
                .user(member)
                .familyId(null)
                .scope(AccountScope.PERSONAL)
                .name("Ví cá nhân")
                .type(AccountType.CASH)
                .balance(BigDecimal.ZERO)
                .currency("VND")
                .build();

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(accountRepository.findById(7L)).thenReturn(Optional.of(personalAccount));

        assertThatThrownBy(() -> sharedTransactionService.createSharedTransaction(
                        "member@test.com", GROUP_ID, createRequest(7L, new BigDecimal("100000"), "x")))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("tài khoản chung");
    }

    @Test
    void createSharedTxn_wrongGroupAccount() {
        User member = user(MEMBER_ID, "member@test.com", "Member");
        Account otherGroupAccount = sharedAccount(8L, OTHER_GROUP_ID, member);

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(accountRepository.findById(8L)).thenReturn(Optional.of(otherGroupAccount));

        assertThatThrownBy(() -> sharedTransactionService.createSharedTransaction(
                        "member@test.com", GROUP_ID, createRequest(8L, new BigDecimal("100000"), "x")))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("tài khoản chung");
    }

    @Test
    void getSharedTxns_personalNotLeaked() {
        User member = user(MEMBER_ID, "member@test.com", "Member B");
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(transactionRepository.countSharedTransactionsWithFilters(
                        eq(GROUP_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(1L);
        when(transactionRepository.findSharedTransactionsWithFilters(
                        eq(GROUP_ID), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(sharedTxn(50L, member, GROUP_ID)));

        PagedResponse<SharedTransactionResponse> page =
                sharedTransactionService.getSharedTransactions("member@test.com", GROUP_ID, null, null, null, 0, 20);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getScope()).isEqualTo(TransactionScope.SHARED);
        verify(transactionRepository)
                .findSharedTransactionsWithFilters(
                        eq(GROUP_ID), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getSharedTxns_includesCreatedBy() {
        User creator = user(ADMIN_ID, "admin@test.com", "Nguyen Van A");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(creator));
        when(transactionRepository.countSharedTransactionsWithFilters(
                        eq(GROUP_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(1L);
        when(transactionRepository.findSharedTransactionsWithFilters(
                        eq(GROUP_ID), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(sharedTxn(60L, creator, GROUP_ID)));

        PagedResponse<SharedTransactionResponse> page =
                sharedTransactionService.getSharedTransactions("admin@test.com", GROUP_ID, null, null, null, 0, 20);

        assertThat(page.getContent().get(0).getCreatedByUserId()).isEqualTo(ADMIN_ID);
        assertThat(page.getContent().get(0).getCreatedByName()).isEqualTo("Nguyen Van A");
    }

    @Test
    void getSharedTxns_nonMemberForbidden() {
        User outsider = user(OUTSIDER_ID, "outsider@test.com", "User C");
        when(userRepository.findByEmail("outsider@test.com")).thenReturn(Optional.of(outsider));
        doThrow(new ForbiddenException("Bạn không phải thành viên của nhóm này"))
                .when(groupAuthorizationService)
                .requireMember(GROUP_ID, OUTSIDER_ID);

        assertThatThrownBy(() -> sharedTransactionService.getSharedTransactions(
                        "outsider@test.com", GROUP_ID, null, null, null, 0, 20))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteSharedTxn_byCreator() {
        User member = user(MEMBER_ID, "member@test.com", "Member B");
        Transaction txn = sharedTxn(70L, member, GROUP_ID);

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(transactionRepository.findByIdAndFamilyIdAndScopeAndDeletedAtIsNull(
                        70L, GROUP_ID, TransactionScope.SHARED))
                .thenReturn(Optional.of(txn));

        sharedTransactionService.deleteSharedTransaction("member@test.com", GROUP_ID, 70L);

        assertThat(txn.getDeletedAt()).isNotNull();
        verify(transactionRepository).save(txn);
    }

    @Test
    void deleteSharedTxn_byAdmin() {
        User admin = user(ADMIN_ID, "admin@test.com", "Admin A");
        User member = user(MEMBER_ID, "member@test.com", "Member B");
        Transaction txn = sharedTxn(71L, member, GROUP_ID);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(transactionRepository.findByIdAndFamilyIdAndScopeAndDeletedAtIsNull(
                        71L, GROUP_ID, TransactionScope.SHARED))
                .thenReturn(Optional.of(txn));
        when(groupAuthorizationService.isAdmin(GROUP_ID, ADMIN_ID)).thenReturn(true);

        sharedTransactionService.deleteSharedTransaction("admin@test.com", GROUP_ID, 71L);

        assertThat(txn.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteSharedTxn_byOtherMemberForbidden() {
        User member = user(MEMBER_ID, "member@test.com", "Member B");
        User otherMember = user(4L, "other@test.com", "Other");
        Transaction txn = sharedTxn(72L, otherMember, GROUP_ID);

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(transactionRepository.findByIdAndFamilyIdAndScopeAndDeletedAtIsNull(
                        72L, GROUP_ID, TransactionScope.SHARED))
                .thenReturn(Optional.of(txn));
        when(groupAuthorizationService.isAdmin(GROUP_ID, MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> sharedTransactionService.deleteSharedTransaction("member@test.com", GROUP_ID, 72L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("ADMIN");
    }

    private static User user(Long id, String email, String fullName) {
        return User.builder().id(id).email(email).fullName(fullName).build();
    }

    private static Account sharedAccount(Long id, Long groupId, User owner) {
        return Account.builder()
                .id(id)
                .user(owner)
                .familyId(groupId)
                .scope(AccountScope.SHARED)
                .name("Quỹ gia đình")
                .type(AccountType.CASH)
                .balance(new BigDecimal("10000000"))
                .currency("VND")
                .build();
    }

    private static Transaction sharedTxn(Long id, User creator, Long groupId) {
        Account account = sharedAccount(5L, groupId, creator);
        return Transaction.builder()
                .id(id)
                .user(creator)
                .account(account)
                .familyId(groupId)
                .scope(TransactionScope.SHARED)
                .amount(new BigDecimal("200000"))
                .type(TransactionType.EXPENSE)
                .description("Mua rau")
                .transactionDate(LocalDate.of(2026, 5, 15))
                .autoCategorized(false)
                .flaggedAnomaly(false)
                .build();
    }

    private static TransactionCreateRequest createRequest(Long accountId, BigDecimal amount, String description) {
        TransactionCreateRequest req = new TransactionCreateRequest();
        req.setAccountId(accountId);
        req.setAmount(amount);
        req.setType(TransactionType.EXPENSE);
        req.setDescription(description);
        req.setTransactionDate(LocalDate.of(2026, 5, 15));
        return req;
    }
}
