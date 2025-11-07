package com.triasoft.garage.controller;

import com.triasoft.garage.model.product.ProductRq;
import com.triasoft.garage.model.product.ProductRs;
import com.triasoft.garage.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductRs> getProducts() {
        return ResponseEntity.ok(productService.getProducts(ProductRq.builder().build()));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductRs> getProduct(@PathVariable("id") Long id) {
        return ResponseEntity.ok(productService.getProduct(ProductRq.builder().id(id).build()));
    }

    @GetMapping(value = "/filter", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductRs> getProducts(@RequestParam(name = "name", required = false) String name, @RequestParam(name = "categoryId", required = false) Long categoryId, @RequestParam(name = "brandId", required = false) Long brandId, @RequestParam(name = "modelId", required = false) Long modelId, @RequestParam(name = "varientId", required = false) Long varientId) {
        return ResponseEntity.ok(productService.getProducts(ProductRq.builder().name(name).categoryId(categoryId).brandId(brandId).modelId(modelId).varientId(varientId).build()));
    }

    @GetMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductRs> getCategories() {
        return ResponseEntity.ok(productService.getCategories(ProductRq.builder().build()));
    }

    @GetMapping(value = "/categories/{id}/brands", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductRs> getBrands(@PathVariable("id") Long categoryId) {
        return ResponseEntity.ok(productService.getBrands(ProductRq.builder().categoryId(categoryId).build()));
    }

    @GetMapping(value = "/categories/{categoryId}/brands/{id}/models", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductRs> getModels(@PathVariable("categoryId") Long categoryId, @PathVariable("id") Long brandId) {
        return ResponseEntity.ok(productService.getModels(ProductRq.builder().categoryId(categoryId).brandId(brandId).build()));
    }

    @GetMapping(value = "/categories/{categoryId}/brands/{brandId}/models/{id}/varients", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductRs> getVarients(@PathVariable("categoryId") Long categoryId, @PathVariable("brandId") Long brandId, @PathVariable("id") Long modelId) {
        return ResponseEntity.ok(productService.getVarients(ProductRq.builder().categoryId(categoryId).brandId(brandId).modelId(modelId).build()));
    }

}
