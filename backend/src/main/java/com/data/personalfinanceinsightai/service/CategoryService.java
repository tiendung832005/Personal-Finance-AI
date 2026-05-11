package com.data.personalfinanceinsightai.service;

import com.data.personalfinanceinsightai.dto.response.category.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> listForUser(String email);

    CategoryResponse getById(String email, Long id);
}
