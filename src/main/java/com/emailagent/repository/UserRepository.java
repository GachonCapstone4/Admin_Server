package com.emailagent.repository;

import com.emailagent.domain.entity.User;
import com.emailagent.domain.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // 관리자 - 전체 조회 (role 필터)
    Page<User> findByRole(UserRole role, Pageable pageable);

    // 관리자 - role 필터 카운트 (대시보드용)
    long countByRole(UserRole role);

    // 관리자 - 이름 검색 (role 필터 + 페이징)
    Page<User> findByRoleAndNameContainingIgnoreCase(UserRole role, String name, Pageable pageable);

    // 관리자 - 이메일 검색 (role 필터 + 페이징)
    Page<User> findByRoleAndEmailContainingIgnoreCase(UserRole role, String email, Pageable pageable);

    // 관리자 - 업종 검색: BusinessProfiles JOIN (native, USER role만)
    @Query(value = "SELECT u.* FROM users u LEFT JOIN business_profiles bp ON u.user_id = bp.user_id WHERE u.role = 'USER' AND bp.industry_type LIKE CONCAT('%', :keyword, '%') ORDER BY u.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM users u LEFT JOIN business_profiles bp ON u.user_id = bp.user_id WHERE u.role = 'USER' AND bp.industry_type LIKE CONCAT('%', :keyword, '%')",
           nativeQuery = true)
    Page<User> findByIndustryTypeKeyword(@Param("keyword") String keyword, Pageable pageable);
}
