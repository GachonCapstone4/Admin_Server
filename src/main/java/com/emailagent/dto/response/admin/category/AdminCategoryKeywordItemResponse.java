package com.emailagent.dto.response.admin.category;

import com.emailagent.domain.entity.Category;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminCategoryKeywordItemResponse {

    @JsonProperty("category_id")
    private final Long categoryId;

    @JsonProperty("user_id")
    private final Long userId;

    @JsonProperty("user_email")
    private final String userEmail;

    @JsonProperty("user_name")
    private final String userName;

    @JsonProperty("category_name")
    private final String categoryName;

    private final String color;

    private final List<String> keywords;

    public AdminCategoryKeywordItemResponse(Category category) {
        this.categoryId = category.getCategoryId();
        this.userId = category.getUser().getUserId();
        this.userEmail = category.getUser().getEmail();
        this.userName = category.getUser().getName();
        this.categoryName = category.getCategoryName();
        this.color = category.getColor();
        this.keywords = category.getKeywords();
    }
}
