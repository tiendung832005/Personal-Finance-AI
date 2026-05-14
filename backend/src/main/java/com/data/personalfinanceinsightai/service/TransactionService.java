package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.transaction.TransactionCreateRequest;
import com.data.personalfinanceinsightai.dto.request.transaction.TransactionUpdateRequest;
import com.data.personalfinanceinsightai.dto.response.transaction.TransactionResponse;
import com.data.personalfinanceinsightai.entity.enums.TransactionType;
import java.util.List;

public interface TransactionService {

    TransactionResponse create(String email, TransactionCreateRequest request);

    List<TransactionResponse> listForUser(String email, String month, Long categoryId, TransactionType type);

    TransactionResponse getById(String email, Long id);

    TransactionResponse update(String email, Long id, TransactionUpdateRequest request);

    void softDelete(String email, Long id);
}
