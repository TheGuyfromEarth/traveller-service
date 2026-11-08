package com.travolish.traveller.catalog.service;

import com.travolish.traveller.catalog.entity.CatalogItem;
import com.travolish.traveller.catalog.repository.CatalogItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final CatalogItemRepository repository;

    public List<CatalogItem> findAll(String type, String status) {
        if (type != null && status != null) {
            return repository.findByItemTypeAndStatusOrderByDisplayOrderAsc(type, status);
        } else if (type != null) {
            return repository.findByItemTypeOrderByDisplayOrderAsc(type);
        } else if (status != null) {
            return repository.findByStatusOrderByDisplayOrderAsc(status);
        }
        return repository.findAllByOrderByItemTypeAscDisplayOrderAsc();
    }

    public Optional<CatalogItem> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public CatalogItem create(CatalogItem item) {
        if (item.getStatus() == null) item.setStatus("ACTIVE");
        if (item.getDisplayOrder() == null) item.setDisplayOrder(0);
        if (item.getUsageCount() == null) item.setUsageCount(0);
        return repository.save(item);
    }

    @Transactional
    public Optional<CatalogItem> update(Long id, CatalogItem incoming) {
        return repository.findById(id).map(existing -> {
            existing.setName(incoming.getName());
            existing.setItemType(incoming.getItemType());
            existing.setItemGroup(incoming.getItemGroup());
            existing.setIcon(incoming.getIcon());
            existing.setStatus(incoming.getStatus());
            existing.setDisplayOrder(incoming.getDisplayOrder());
            return repository.save(existing);
        });
    }

    @Transactional
    public Optional<CatalogItem> toggleStatus(Long id, String newStatus) {
        return repository.findById(id).map(item -> {
            item.setStatus(newStatus);
            return repository.save(item);
        });
    }

    @Transactional
    public Optional<CatalogItem> updateOrder(Long id, Integer displayOrder) {
        return repository.findById(id).map(item -> {
            item.setDisplayOrder(displayOrder);
            return repository.save(item);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }
}
