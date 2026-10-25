package com.travolish.traveller.catalog.repository;

import com.travolish.traveller.catalog.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    List<CatalogItem> findByItemTypeOrderByDisplayOrderAsc(String itemType);
    List<CatalogItem> findByStatusOrderByDisplayOrderAsc(String status);
    List<CatalogItem> findByItemTypeAndStatusOrderByDisplayOrderAsc(String itemType, String status);
    List<CatalogItem> findAllByOrderByItemTypeAscDisplayOrderAsc();
}
