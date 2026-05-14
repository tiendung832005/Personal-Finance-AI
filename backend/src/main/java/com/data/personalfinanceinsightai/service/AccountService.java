package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.account.AccountCreateRequest;
import com.data.personalfinanceinsightai.dto.request.account.AccountUpdateRequest;
import com.data.personalfinanceinsightai.dto.response.account.AccountResponse;
import java.util.List;

public interface AccountService {

    List<AccountResponse> listForUser(String email);

    AccountResponse create(String email, AccountCreateRequest request);

    AccountResponse getById(String email, Long id);

    AccountResponse update(String email, Long id, AccountUpdateRequest request);

    /**
     * @return {@code "soft"} if archived because transactions exist, {@code "hard"} if physically removed
     */
    String deleteAccount(String email, Long id);
}
