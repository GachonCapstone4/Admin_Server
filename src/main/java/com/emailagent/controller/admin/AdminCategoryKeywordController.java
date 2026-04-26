package com.emailagent.controller.admin;

import com.emailagent.dto.request.admin.AdminCategoryKeywordCreateRequest;
import com.emailagent.dto.request.admin.AdminCategoryKeywordUpdateRequest;
import com.emailagent.dto.response.admin.AdminSimpleResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordItemResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordListResponse;
import com.emailagent.service.admin.AdminCategoryKeywordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryKeywordController {

    private final AdminCategoryKeywordService adminCategoryKeywordService;

    @GetMapping
    public AdminCategoryKeywordListResponse getCategories(
            @RequestParam(value = "user_id", required = false) Long userId) {
        return adminCategoryKeywordService.getCategories(userId);
    }

    @PostMapping
    public AdminCategoryKeywordItemResponse createCategory(
            @Valid @RequestBody AdminCategoryKeywordCreateRequest request) {
        return adminCategoryKeywordService.createCategory(request);
    }

    @PatchMapping("/{category_id}")
    public AdminCategoryKeywordItemResponse updateCategory(
            @PathVariable("category_id") Long categoryId,
            @Valid @RequestBody AdminCategoryKeywordUpdateRequest request) {
        return adminCategoryKeywordService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{category_id}")
    public AdminSimpleResponse deleteCategory(@PathVariable("category_id") Long categoryId) {
        return adminCategoryKeywordService.deleteCategory(categoryId);
    }
}
