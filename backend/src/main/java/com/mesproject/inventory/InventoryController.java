package com.mesproject.inventory;

import com.mesproject.service.InventoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventories")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable Long productId) {
        return inventoryService.getByProductId(productId);
    }
}
