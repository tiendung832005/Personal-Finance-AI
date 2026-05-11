package com.data.personalfinanceinsightai.dto.response.category;

import com.data.personalfinanceinsightai.entity.Category;
import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String icon;
    private CategoryType type;
    private String color;

    @JsonProperty("isSystem")
    private boolean system;

    private Long userId;

    public static CategoryResponse fromEntity(Category category) {
        Long ownerId = category.getUser() == null ? null : category.getUser().getId();
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .type(category.getType())
                .color(category.getColor())
                .system(Boolean.TRUE.equals(category.getBuiltIn()))
                .userId(ownerId)
                .build();
    }
}
