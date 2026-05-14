package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.request.category.CategoryCreateRequest;
import com.data.personalfinanceinsightai.dto.response.category.CategoryResponse;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> listForUser(String email, CategoryType type);

    CategoryResponse getById(String email, Long id);

    CategoryResponse createCustom(String email, CategoryCreateRequest request);
}
