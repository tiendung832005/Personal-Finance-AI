package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.account.AccountCreateRequest;
import com.data.personalfinanceinsightai.dto.response.account.AccountResponse;
import java.util.List;

public interface AccountService {

    List<AccountResponse> listForUser(String email);

    AccountResponse create(String email, AccountCreateRequest request);
}
