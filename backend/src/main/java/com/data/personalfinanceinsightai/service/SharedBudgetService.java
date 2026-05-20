package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.budget.BudgetCreateRequest;
import com.data.personalfinanceinsightai.dto.request.budget.BudgetUpdateRequest;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetResponse;
import com.data.personalfinanceinsightai.dto.response.budget.BudgetStatusResponse;
import java.util.List;

public interface SharedBudgetService {

    BudgetResponse createGroupBudget(String email, Long groupId, BudgetCreateRequest request);

    List<BudgetResponse> listGroupBudgets(String email, Long groupId, String month);

    BudgetStatusResponse getGroupBudgetStatus(String email, Long groupId, String month);

    BudgetResponse updateGroupBudget(String email, Long groupId, Long budgetId, BudgetUpdateRequest request);

    void deleteGroupBudget(String email, Long groupId, Long budgetId);
}
