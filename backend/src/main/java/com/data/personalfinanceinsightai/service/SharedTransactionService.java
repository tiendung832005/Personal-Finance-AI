package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.transaction.TransactionCreateRequest;
import com.data.personalfinanceinsightai.dto.response.PagedResponse;
import com.data.personalfinanceinsightai.dto.response.transaction.SharedTransactionResponse;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;

public interface SharedTransactionService {

    SharedTransactionResponse createSharedTransaction(
            String email, Long groupId, TransactionCreateRequest request);

    PagedResponse<SharedTransactionResponse> getSharedTransactions(
            String email, Long groupId, String month, Long categoryId, TransactionType type, int page, int size);

    void deleteSharedTransaction(String email, Long groupId, Long transactionId);
}
