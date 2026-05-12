package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.transaction.TransactionCreateRequest;
import com.data.personalfinanceinsightai.dto.response.transaction.TransactionResponse;

public interface TransactionService {

    TransactionResponse create(String email, TransactionCreateRequest request);
}
