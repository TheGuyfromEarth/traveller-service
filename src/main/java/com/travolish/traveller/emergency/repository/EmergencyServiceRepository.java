package com.travolish.traveller.emergency.repository;

import com.travolish.traveller.emergency.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for hotel-attached and city-level public emergency services
 * (fire brigade, police, hospitals, etc.).
 *
 * <p>This is intentionally named {@code EmergencyServiceRepository}, not
 * {@code EmergencyContactRepository}, to avoid a Spring bean-name collision with
 * {@code com.travolish.traveller.user.repository.EmergencyContactRepository}, which
 * stores a traveller's personal ICE (In Case of Emergency) contacts.
 */
@Repository
public interface EmergencyServiceRepository extends JpaRepository<EmergencyContact, Long> {

    List<EmergencyContact> findByCountry(String country);

    List<EmergencyContact> findByCity(String city);

    List<EmergencyContact> findByCountryAndCity(String country, String city);

    List<EmergencyContact> findByContactType(String contactType);

    // JPQL entity name must match @Entity(name = "EmergencyService") on the entity class
    @Query("SELECT ec FROM EmergencyService ec WHERE ec.country = :country AND ec.city = :city AND ec.isActive = true")
    List<EmergencyContact> findActiveByLocation(String country, String city);

    @Query("SELECT ec FROM EmergencyService ec WHERE ec.country = :country AND ec.contactType = :type AND ec.isActive = true")
    List<EmergencyContact> findActiveByTypeAndCountry(String country, String type);

    @Query("SELECT ec FROM EmergencyService ec WHERE ec.isActive = true ORDER BY ec.responseTimeMinutes ASC")
    List<EmergencyContact> findAllActiveOrderedByResponseTime();

    List<EmergencyContact> findByHotelIdAndIsActiveTrue(Long hotelId);
}
