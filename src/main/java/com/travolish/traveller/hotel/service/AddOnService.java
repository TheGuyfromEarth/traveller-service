package com.travolish.traveller.hotel.service;

import java.util.List;

import com.travolish.traveller.hotel.dto.AddOnDTO;

public interface AddOnService {
    List<AddOnDTO> findByHotelId(Long hotelId);
    AddOnDTO create(AddOnDTO dto);
    AddOnDTO update(Long id, AddOnDTO dto);
    void delete(Long id);
}
