package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.account.CreateSharedAccountRequest;
import com.data.personalfinanceinsightai.dto.response.account.AccountResponse;
import java.util.List;

public interface SharedAccountService {

    AccountResponse createSharedAccount(String email, Long groupId, CreateSharedAccountRequest request);

    List<AccountResponse> getSharedAccounts(String email, Long groupId);
}
