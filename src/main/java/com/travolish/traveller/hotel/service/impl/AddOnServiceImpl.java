package com.travolish.traveller.hotel.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.travolish.traveller.hotel.dto.AddOnDTO;
import com.travolish.traveller.hotel.model.AddOn;
import com.travolish.traveller.hotel.repository.AddOnRepository;
import com.travolish.traveller.hotel.service.AddOnService;

@Service
public class AddOnServiceImpl implements AddOnService {

    private final AddOnRepository addOnRepository;

    public AddOnServiceImpl(AddOnRepository addOnRepository) {
        this.addOnRepository = addOnRepository;
    }

    @Override
    public List<AddOnDTO> findByHotelId(Long hotelId) {
        return addOnRepository.findByHotelId(hotelId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AddOnDTO create(AddOnDTO dto) {
        AddOn addOn = toEntity(dto);
        addOn.setId(null);
        return toDTO(addOnRepository.save(addOn));
    }

    @Override
    public AddOnDTO update(Long id, AddOnDTO dto) {
        AddOn existing = addOnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AddOn not found: " + id));
        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setPrice(dto.getPrice());
        return toDTO(addOnRepository.save(existing));
    }

    @Override
    public void delete(Long id) {
        addOnRepository.deleteById(id);
    }

    private AddOnDTO toDTO(AddOn addOn) {
        return AddOnDTO.builder()
                .id(addOn.getId())
                .title(addOn.getTitle())
                .description(addOn.getDescription())
                .price(addOn.getPrice())
                .hotelId(addOn.getHotelId())
                .build();
    }

    private AddOn toEntity(AddOnDTO dto) {
        AddOn addOn = new AddOn();
        addOn.setId(dto.getId());
        addOn.setTitle(dto.getTitle());
        addOn.setDescription(dto.getDescription());
        addOn.setPrice(dto.getPrice());
        addOn.setHotelId(dto.getHotelId());
        return addOn;
    }
}
