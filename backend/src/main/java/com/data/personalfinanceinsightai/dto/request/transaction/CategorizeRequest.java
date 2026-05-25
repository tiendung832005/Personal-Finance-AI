package com.data.personalfinanceinsightai.dto.request.transaction;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategorizeRequest {
    
    @NotBlank(message = "Description is required")
    private String description;
}
