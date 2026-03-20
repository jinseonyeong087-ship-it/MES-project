package com.mesproject.service;

import com.mesproject.entity.Inventory;
import com.mesproject.inventory.InventoryResponse;
import com.mesproject.repo.InventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryResponse getByProductId(Long productId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));

        return new InventoryResponse(
                inv.getProduct().getId(),
                inv.getProduct().getProductCode(),
                inv.getProduct().getProductName(),
                inv.getQtyOnHand(),
                inv.getSafetyStock(),
                inv.getUpdatedAt()
        );
    }
}
