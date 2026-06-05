package com.travolish.traveller.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.user.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    // LIMIT 1 guards against duplicate email rows created by test runs
    @Query("SELECT u FROM User u WHERE u.email = :email ORDER BY u.id ASC LIMIT 1")
    Optional<User> findByEmail(@Param("email") String email);
    Optional<User> findBySupabaseId(String supabaseId);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // Null-aware: treat NULL role as GUEST, NULL status as ACTIVE
    @Query("SELECT u FROM User u WHERE u.role = :role OR (:role = 'GUEST' AND u.role IS NULL)")
    List<User> findByRoleWithDefault(@Param("role") String role);

    @Query("SELECT u FROM User u WHERE u.status = :status OR (:status = 'ACTIVE' AND u.status IS NULL)")
    List<User> findByStatusWithDefault(@Param("status") String status);

    @Query("SELECT u FROM User u WHERE (u.role = :role OR (:role = 'GUEST' AND u.role IS NULL)) AND (u.status = :status OR (:status = 'ACTIVE' AND u.status IS NULL))")
    List<User> findByRoleAndStatusWithDefault(@Param("role") String role, @Param("status") String status);
}
