package com.travolish.traveller.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.booking.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

}
