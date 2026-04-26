package com.emailagent.service.admin;

import com.emailagent.domain.entity.Category;
import com.emailagent.domain.entity.CategoryKeywordRule;
import com.emailagent.domain.entity.Template;
import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.RagTemplateIndexRequestDTO;
import com.emailagent.repository.BusinessProfileRepository;
import com.emailagent.repository.CategoryKeywordRuleRepository;
import com.emailagent.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagTemplateIndexService {

    private final TemplateRepository templateRepository;
    private final BusinessProfileRepository profileRepository;
    private final CategoryKeywordRuleRepository keywordRuleRepository;
    private final RabbitTemplate rabbitTemplate;

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

        RagTemplateIndexRequestDTO message = RagTemplateIndexRequestDTO.builder()
                .requestId(requestId)
                .userId(category.getUser().getUserId())
                .payload(RagTemplateIndexRequestDTO.Payload.builder()
                        .templates(indexItems)
                        .build())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_APP2RAG,
                RabbitMQConfig.RK_TEMPLATE_INDEX_INBOUND,
                message,
                new CorrelationData(requestId)
        );

        log.info(
                "[RagTemplateIndexService] template index 발행 완료 - userId={}, requestId={}",
                message.getUserId(),
                requestId
        );
    }

    private RagTemplateIndexRequestDTO.TemplateItem toIndexItem(
            Template template,
            Category category,
            String emailTone
    ) {
        return RagTemplateIndexRequestDTO.TemplateItem.builder()
                .templateId(template.getTemplateId())
                .title(template.getTitle())
                .categoryName(category.getCategoryName())
                .emailTone(emailTone)
                .metadata(RagTemplateIndexRequestDTO.Metadata.builder()
                        .searchSummary("일반형 템플릿")
                        .semanticKeywords(toSemanticKeywords(category, "일반형"))
                        .recommendedSituations(List.of())
                        .build())
                .build();
    }

    public List<String> toSemanticKeywords(Category category, String variantLabel) {
        List<String> semanticKeywords = new ArrayList<>();
        semanticKeywords.add(category.getCategoryName());
        semanticKeywords.add(variantLabel);
        semanticKeywords.addAll(resolveCategoryKeywords(category.getCategoryName()));
        return normalizeKeywords(semanticKeywords);
    }

    public List<String> resolveCategoryKeywords(String categoryName) {
        return keywordRuleRepository.findByCategoryName(categoryName)
                .map(CategoryKeywordRule::getKeywords)
                .orElse(List.of());
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
