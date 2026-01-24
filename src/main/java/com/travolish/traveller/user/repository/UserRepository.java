package com.travolish.traveller.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.user.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    java.util.Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
