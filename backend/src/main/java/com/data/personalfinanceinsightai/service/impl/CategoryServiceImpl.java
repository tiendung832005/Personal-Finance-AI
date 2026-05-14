package com.data.personalfinanceinsightai.service.impl;

import com.data.personalfinanceinsightai.dto.request.category.CategoryCreateRequest;
import com.data.personalfinanceinsightai.dto.response.category.CategoryResponse;
import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.User;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.data.personalfinanceinsightai.exception.ResourceNotFoundException;
import com.data.personalfinanceinsightai.repository.CategoryRepository;
import com.data.personalfinanceinsightai.repository.UserRepository;
import com.data.personalfinanceinsightai.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listForUser(String email, CategoryType type) {
        Long userId = resolveUserId(email);
        return categoryRepository.findAllVisibleForUser(userId, type).stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(String email, Long id) {
        Long userId = resolveUserId(email);
        Category category = categoryRepository
                .findVisibleByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCustom(String email, CategoryCreateRequest request) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String name = request.getName().trim();
        if (categoryRepository.existsByUser_IdAndNameIgnoreCase(user.getId(), name)) {
            throw new IllegalArgumentException("A custom category with this name already exists");
        }

        Category category = Category.builder()
                .name(name)
                .icon(trimToNull(request.getIcon()))
                .type(request.getType())
                .color(trimToNull(request.getColor()))
                .builtIn(false)
                .user(user)
                .build();

        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    private Long resolveUserId(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
