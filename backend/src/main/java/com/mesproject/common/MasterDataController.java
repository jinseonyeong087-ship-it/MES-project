package com.mesproject.common;

import com.mesproject.repo.ProcessRepository;
import com.mesproject.repo.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MasterDataController {
    private final ProductRepository productRepository;
    private final ProcessRepository processRepository;

    public MasterDataController(ProductRepository productRepository, ProcessRepository processRepository) {
        this.productRepository = productRepository;
        this.processRepository = processRepository;
    }

    @GetMapping("/products")
    public List<CodeItemResponse> getProducts() {
        return productRepository.findAllByOrderByIdAsc().stream()
                .map(p -> new CodeItemResponse(p.getId(), p.getProductCode(), p.getProductName()))
                .toList();
    }

    @GetMapping("/processes")
    public List<CodeItemResponse> getProcesses() {
        return processRepository.findAllByOrderByIdAsc().stream()
                .map(p -> new CodeItemResponse(p.getId(), p.getProcessCode(), p.getProcessName()))
                .toList();
    }
}
