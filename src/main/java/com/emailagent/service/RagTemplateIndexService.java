package com.emailagent.service;

import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.Template;
import com.emailagent.rabbitmq.dto.RagTemplateIndexRequestDTO;
import com.emailagent.rabbitmq.publisher.RagTemplateIndexPublisher;
import com.emailagent.repository.BusinessProfileRepository;
import com.emailagent.repository.TemplateRepository;
import com.emailagent.util.CategoryKeywordDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagTemplateIndexService {

    private final TemplateRepository templateRepository;
    private final BusinessProfileRepository profileRepository;
    private final RagTemplateIndexPublisher ragTemplateIndexPublisher;

    public void reindexCategories(List<Category> categories) {
        Map<Long, Category> uniqueCategories = new LinkedHashMap<>();
        categories.forEach(category -> uniqueCategories.put(category.getCategoryId(), category));
        uniqueCategories.values().forEach(this::reindexCategory);
    }

    public void reindexCategory(Category category) {
        List<Template> templates = templateRepository.findByCategory_CategoryId(category.getCategoryId());
        if (templates.isEmpty()) {
            return;
        }

        String requestId = "template-index-" + category.getCategoryId() + "-" + System.currentTimeMillis();
        String emailTone = profileRepository.findByUser_UserId(category.getUser().getUserId())
                .map(profile -> profile.getEmailTone() != null ? profile.getEmailTone().name() : null)
                .orElse(null);

        List<RagTemplateIndexRequestDTO.TemplateItem> indexItems = templates.stream()
                .map(template -> toIndexItem(template, category, emailTone))
                .toList();

        ragTemplateIndexPublisher.publish(RagTemplateIndexRequestDTO.builder()
                .requestId(requestId)
                .userId(category.getUser().getUserId())
                .payload(RagTemplateIndexRequestDTO.Payload.builder()
                        .templates(indexItems)
                        .build())
                .build());
    }

    private RagTemplateIndexRequestDTO.TemplateItem toIndexItem(
            Template template,
            Category category,
            String emailTone
    ) {
        List<String> semanticKeywords = new ArrayList<>();
        semanticKeywords.add(category.getCategoryName());
        semanticKeywords.add("일반형");
        semanticKeywords.addAll(CategoryKeywordDefaults.resolve(category.getCategoryName(), category.getKeywords()));

        return RagTemplateIndexRequestDTO.TemplateItem.builder()
                .templateId(template.getTemplateId())
                .title(template.getTitle())
                .categoryName(category.getCategoryName())
                .emailTone(emailTone)
                .metadata(RagTemplateIndexRequestDTO.Metadata.builder()
                        .searchSummary("일반형 템플릿")
                        .semanticKeywords(CategoryKeywordDefaults.normalize(semanticKeywords))
                        .recommendedSituations(List.of())
                        .build())
                .build();
    }
}
