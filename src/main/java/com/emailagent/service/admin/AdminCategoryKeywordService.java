package com.emailagent.service.admin;

import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.User;
import com.emailagent.dto.request.admin.AdminCategoryKeywordCreateRequest;
import com.emailagent.dto.request.admin.AdminCategoryKeywordUpdateRequest;
import com.emailagent.dto.response.admin.AdminSimpleResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordItemResponse;
import com.emailagent.dto.response.admin.category.AdminCategoryKeywordListResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.repository.CategoryRepository;
import com.emailagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCategoryKeywordService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminCategoryKeywordListResponse getCategories(Long userId) {
        List<Category> categories = userId == null
                ? categoryRepository.findAllWithUserOrderByUserIdAndCategoryName()
                : categoryRepository.findByUserIdWithUserOrderByCategoryName(userId);

        return new AdminCategoryKeywordListResponse(
                categories.stream()
                        .map(AdminCategoryKeywordItemResponse::new)
                        .toList()
        );
    }

    @Transactional
    public AdminCategoryKeywordItemResponse createCategory(AdminCategoryKeywordCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. userId=" + request.getUserId()));

        String categoryName = normalizeRequired(request.getCategoryName(), "카테고리명은 필수입니다.");
        if (categoryRepository.existsByUser_UserIdAndCategoryName(user.getUserId(), categoryName)) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + categoryName);
        }

        Category category = Category.builder()
                .user(user)
                .categoryName(categoryName)
                .color(normalizeOptional(request.getColor()))
                .keywords(normalizeKeywords(request.getKeywords()))
                .build();

        return new AdminCategoryKeywordItemResponse(categoryRepository.save(category));
    }

    @Transactional
    public AdminCategoryKeywordItemResponse updateCategory(Long categoryId, AdminCategoryKeywordUpdateRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다. categoryId=" + categoryId));

        String categoryName = normalizeRequired(request.getCategoryName(), "카테고리명은 필수입니다.");
        categoryRepository.findByUser_UserIdAndCategoryName(category.getUser().getUserId(), categoryName)
                .filter(found -> !found.getCategoryId().equals(categoryId))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + categoryName);
                });

        category.updateByAdmin(
                categoryName,
                normalizeOptional(request.getColor()),
                normalizeKeywords(request.getKeywords())
        );

        return new AdminCategoryKeywordItemResponse(category);
    }

    @Transactional
    public AdminSimpleResponse deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("카테고리를 찾을 수 없습니다. categoryId=" + categoryId);
        }
        categoryRepository.deleteById(categoryId);
        return AdminSimpleResponse.OK;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> uniqueKeywords = new LinkedHashSet<>();
        for (String keyword : keywords) {
            String normalized = normalizeOptional(keyword);
            if (normalized != null) {
                uniqueKeywords.add(normalized);
            }
        }

        return new ArrayList<>(uniqueKeywords);
    }
}
