package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ProductTypeEnum;
import com.triasoft.garage.entity.ProductCategory;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.model.product.ProductRq;
import com.triasoft.garage.repository.ProductBrandModelRepository;
import com.triasoft.garage.repository.ProductBrandRepository;
import com.triasoft.garage.repository.ProductCategoryRepository;
import com.triasoft.garage.repository.ProductModelVarientRepository;
import com.triasoft.garage.repository.ProductRepository;
import com.triasoft.garage.repository.ProductSegmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code @Cacheable}/{@code @CacheEvict} wiring on {@link ProductService}
 * actually engages — a plain Mockito unit test can't see this, since caching is applied
 * by a Spring AOP proxy around the real bean. Repositories are still mocked; only the
 * caching infrastructure (and ProductService itself) is real, so no DB is needed.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ProductService.class, ProductServiceCacheTest.CacheTestConfig.class})
class ProductServiceCacheTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager(
                    "productCategories", "productSegments", "productBrands", "productModels", "productVarients");
        }
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean private ProductRepository productRepository;
    @MockBean private ProductCategoryRepository productCategoryRepository;
    @MockBean private ProductSegmentRepository productSegmentRepository;
    @MockBean private ProductBrandRepository productBrandRepository;
    @MockBean private ProductBrandModelRepository productBrandModelRepository;
    @MockBean private ProductModelVarientRepository productModelVarientRepository;
    @MockBean private LookupHelper lookupHelper;

    @BeforeEach
    void clearCaches() {
        // The Spring TestContext caches and reuses this ApplicationContext (and its
        // singleton CacheManager) across test methods, so a stale entry from a previous
        // test would otherwise silently short-circuit the next test's repository call.
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void getCategories_secondCallIsServedFromCacheNotRepository() {
        when(productCategoryRepository.findByActiveTrue()).thenReturn(List.of(new ProductCategory()));

        productService.getCategories(ProductRq.builder().build());
        productService.getCategories(ProductRq.builder().build());
        productService.getCategories(ProductRq.builder().build());

        verify(productCategoryRepository, times(1)).findByActiveTrue();
    }

    @Test
    void getBrands_differentCategoryIds_areCachedSeparately() {
        when(productBrandRepository.findByProductCategoryIdAndActiveTrue(1L)).thenReturn(List.of());
        when(productBrandRepository.findByProductCategoryIdAndActiveTrue(2L)).thenReturn(List.of());

        productService.getBrands(ProductRq.builder().categoryId(1L).build());
        productService.getBrands(ProductRq.builder().categoryId(1L).build());
        productService.getBrands(ProductRq.builder().categoryId(2L).build());

        verify(productBrandRepository, times(1)).findByProductCategoryIdAndActiveTrue(1L);
        verify(productBrandRepository, times(1)).findByProductCategoryIdAndActiveTrue(2L);
    }

    @Test
    void manageProductTypes_evictsCategoriesCacheSoNextReadHitsRepositoryAgain() {
        when(productCategoryRepository.findByActiveTrue()).thenReturn(List.of(new ProductCategory()));
        ProductCategory existing = new ProductCategory();
        existing.setId(1L);
        when(productCategoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.getCategories(ProductRq.builder().build()); // populates cache

        productService.manageProductTypes(
                ProductRq.builder().type(ProductTypeEnum.CATEGORY).id(1L).code("C1").description("Category 1").build(),
                null);

        productService.getCategories(ProductRq.builder().build()); // must re-hit the repository

        verify(productCategoryRepository, times(2)).findByActiveTrue();
    }
}
