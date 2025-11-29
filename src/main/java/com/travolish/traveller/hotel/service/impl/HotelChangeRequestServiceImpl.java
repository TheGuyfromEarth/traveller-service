package com.travolish.traveller.hotel.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.model.HotelChangeRequest;
import com.travolish.traveller.hotel.repository.HotelChangeRequestRepository;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.hotel.service.HotelChangeRequestService;

@Service
public class HotelChangeRequestServiceImpl implements HotelChangeRequestService {

    private final HotelChangeRequestRepository requestRepository;
    private final HotelRepository hotelRepository;

    public HotelChangeRequestServiceImpl(HotelChangeRequestRepository requestRepository, HotelRepository hotelRepository) {
        this.requestRepository = requestRepository;
        this.hotelRepository = hotelRepository;
    }

    @Override
    public HotelChangeRequest submit(HotelChangeRequest request) {
        request.setStatus(HotelChangeRequest.RequestStatus.PENDING);
        request.setRequestedAt(OffsetDateTime.now());
        request.setProcessedAt(null);
        return requestRepository.save(request);
    }

    @Override
    public List<HotelChangeRequest> findAll() {
        return requestRepository.findAll();
    }

    @Override
    public List<HotelChangeRequest> findByStatus(HotelChangeRequest.RequestStatus status) {
        return requestRepository.findByStatus(status);
    }

    @Override
    public Optional<HotelChangeRequest> findById(Long id) {
        return requestRepository.findById(id);
    }

    @Override
    @Transactional
    public HotelChangeRequest approve(Long id, String adminComment) {
        var req = requestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (req.getStatus() != HotelChangeRequest.RequestStatus.PENDING) {
            return req;
        }

        if (req.getRequestType() == HotelChangeRequest.RequestType.CREATE) {
            Hotel h = new Hotel();
            h.setName(req.getName());
            h.setAddress(req.getAddress());
            h.setCity(req.getCity());
            h.setRating(req.getRating());
            h.setPhone(req.getPhone());
            h.setEmail(req.getEmail());
            h.setDescription(req.getDescription());
            var saved = hotelRepository.save(h);
            req.setHotelId(saved.getId());
        } else if (req.getRequestType() == HotelChangeRequest.RequestType.UPDATE) {
            if (req.getHotelId() == null) throw new IllegalStateException("Update request missing hotelId");
            var opt = hotelRepository.findById(req.getHotelId());
            if (opt.isPresent()) {
                var existing = opt.get();
                existing.setName(req.getName());
                existing.setAddress(req.getAddress());
                existing.setCity(req.getCity());
                existing.setRating(req.getRating());
                existing.setPhone(req.getPhone());
                existing.setEmail(req.getEmail());
                existing.setDescription(req.getDescription());
                hotelRepository.save(existing);
            } else {
                throw new IllegalStateException("Target hotel not found");
            }
        }

        req.setStatus(HotelChangeRequest.RequestStatus.APPROVED);
        req.setAdminComment(adminComment);
        req.setProcessedAt(OffsetDateTime.now());
        return requestRepository.save(req);
    }

    @Override
    @Transactional
    public HotelChangeRequest reject(Long id, String adminComment) {
        var req = requestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (req.getStatus() != HotelChangeRequest.RequestStatus.PENDING) return req;
        req.setStatus(HotelChangeRequest.RequestStatus.REJECTED);
        req.setAdminComment(adminComment);
        req.setProcessedAt(OffsetDateTime.now());
        return requestRepository.save(req);
    }
}
