package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.budget.BudgetCreateRequest;
import com.data.personalfinanceinsightai.dto.request.budget.BudgetUpdateRequest;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetResponse;
import java.util.List;

public interface BudgetService {

    List<BudgetResponse> listForUser(String email, String month);

    BudgetResponse create(String email, BudgetCreateRequest request);

    BudgetResponse update(String email, Long id, BudgetUpdateRequest request);

    void delete(String email, Long id);
}
