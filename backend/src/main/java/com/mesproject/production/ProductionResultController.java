package com.mesproject.production;

import com.mesproject.service.ProductionResultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/production-results")
public class ProductionResultController {
    private final ProductionResultService productionResultService;

    public ProductionResultController(ProductionResultService productionResultService) {
        this.productionResultService = productionResultService;
    }

    @GetMapping
    public List<ProductionResultListItem> getRecent() {
        return productionResultService.getRecent();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductionResultResponse register(@Valid @RequestBody RegisterProductionResultRequest request) {
        return productionResultService.register(request);
    }
}
