package com.emailagent.repository;

import com.emailagent.domain.entity.EmailAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailAnalysisResultRepository extends JpaRepository<EmailAnalysisResult, Long> {

    Optional<EmailAnalysisResult> findByEmail_EmailId(Long emailId);

    // 카테고리가 매칭된 이메일 수 (템플릿 매칭률 계산용)
    @Query("""
            SELECT COUNT(ar) FROM EmailAnalysisResult ar
            JOIN ar.email e
            WHERE e.user.userId = :userId
              AND ar.category IS NOT NULL
              AND e.receivedAt >= :start
              AND e.receivedAt < :end
            """)
    long countMatchedByUserIdAndDateRange(@Param("userId") Long userId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    // 주간 카테고리별 이메일 수 (N+1 방지: category FETCH JOIN)
    @Query("""
            SELECT ar.category.categoryName, COUNT(ar), ar.category.color
            FROM EmailAnalysisResult ar
            JOIN ar.email e
            JOIN ar.category c
            WHERE e.user.userId = :userId
              AND e.receivedAt >= :start
              AND e.receivedAt < :end
            GROUP BY ar.category.categoryId, ar.category.categoryName, ar.category.color
            ORDER BY COUNT(ar) DESC
            """)
    List<Object[]> findWeeklyCategorySummary(@Param("userId") Long userId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    // 이메일 ID 목록으로 분석 결과 일괄 조회 (N+1 방지)
    @Query("""
            SELECT ar FROM EmailAnalysisResult ar
            LEFT JOIN FETCH ar.category
            WHERE ar.email.emailId IN :emailIds
            """)
    List<EmailAnalysisResult> findByEmailIdsWithCategory(@Param("emailIds") List<Long> emailIds);

    // 관리자 대시보드: 오늘 AI 분석 완료 건수
    @Query("SELECT COUNT(ar) FROM EmailAnalysisResult ar WHERE ar.createdAt >= :start AND ar.createdAt < :end")
    long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 관리자 - 사용자 분석 결과 전체 삭제 (Email에 직접 user 없으므로 서브쿼리 사용)
    @Modifying
    @Query("DELETE FROM EmailAnalysisResult r WHERE r.email.emailId IN (SELECT e.emailId FROM Email e WHERE e.user.userId = :userId)")
    void deleteByUserId(@Param("userId") Long userId);
}
