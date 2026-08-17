package com.travolish.traveller.emergency.repository;

import org.springframework.data.repository.NoRepositoryBean;

/**
 * @deprecated Renamed to {@link EmergencyServiceRepository}.
 *
 *             <p>{@code @NoRepositoryBean} tells Spring Data JPA not to create a bean
 *             implementation for this interface, avoiding the startup conflict with
 *             {@code com.travolish.traveller.user.repository.EmergencyContactRepository}.
 *             Inject {@link EmergencyServiceRepository} directly instead.
 */
@Deprecated(since = "2026-08-15", forRemoval = true)
@NoRepositoryBean
public interface EmergencyContactRepository extends EmergencyServiceRepository {
    // Deprecated alias — all methods live in EmergencyServiceRepository.
}
