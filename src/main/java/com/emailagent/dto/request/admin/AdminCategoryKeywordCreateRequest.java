package com.emailagent.dto.request.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminCategoryKeywordCreateRequest {

    @NotNull
    @JsonProperty("user_id")
    private Long userId;

    @NotBlank
    @JsonProperty("category_name")
    private String categoryName;

    private String color;

    private List<String> keywords;
}
