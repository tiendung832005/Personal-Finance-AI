package com.data.personalfinanceinsightai.dto.request.category;

import com.data.personalfinanceinsightai.entity.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String icon;

    @NotNull
    private CategoryType type;

    @Size(max = 7)
    private String color;
}
