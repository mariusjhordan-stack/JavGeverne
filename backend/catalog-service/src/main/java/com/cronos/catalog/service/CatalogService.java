package com.cronos.catalog.service;

import com.cronos.catalog.dto.*;
import com.cronos.catalog.entity.Brand;
import com.cronos.catalog.entity.Category;
import com.cronos.catalog.entity.Product;
import com.cronos.catalog.repository.BrandRepository;
import com.cronos.catalog.repository.CategoryRepository;
import com.cronos.catalog.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public CatalogService(ProductRepository productRepository,
                          BrandRepository brandRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable).map(this::toProductResponse);
    }

    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        return toProductResponse(product);
    }

    public Page<ProductResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        Specification<Product> spec = buildSpecification(criteria);
        return productRepository.findAll(spec, pageable).map(this::toProductResponse);
    }

    public Page<ProductResponse> getProductsByBrand(Long brandId, Pageable pageable) {
        return productRepository.findByBrandIdAndActiveTrue(brandId, pageable).map(this::toProductResponse);
    }

    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable).map(this::toProductResponse);
    }

    public List<ProductResponse> getFeaturedProducts() {
        return productRepository.findByFeaturedTrueAndActiveTrue().stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new RuntimeException("Brand not found: " + request.brandId()));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.categoryId()));

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setReferenceNumber(request.referenceNumber());
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(request.price());
        product.setCurrency(request.currency() != null ? request.currency() : "USD");
        product.setStock(request.stock() != null ? request.stock() : 0);
        product.setCaseMaterial(request.caseMaterial());
        product.setCaseDiameter(request.caseDiameter());
        if (request.movementType() != null) {
            product.setMovementType(Product.MovementType.valueOf(request.movementType()));
        }
        product.setWaterResistance(request.waterResistance());
        product.setCrystalType(request.crystalType());
        product.setDialColor(request.dialColor());
        product.setStrapMaterial(request.strapMaterial());
        product.setPowerReserve(request.powerReserve());
        product.setImageUrl(request.imageUrl());
        product.setGalleryUrls(request.galleryUrls());
        product.setFeatured(request.featured() != null ? request.featured() : false);
        product.setActive(true);

        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.referenceNumber() != null) product.setReferenceNumber(request.referenceNumber());
        if (request.brandId() != null) {
            Brand brand = brandRepository.findById(request.brandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found: " + request.brandId()));
            product.setBrand(brand);
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + request.categoryId()));
            product.setCategory(category);
        }
        if (request.price() != null) product.setPrice(request.price());
        if (request.currency() != null) product.setCurrency(request.currency());
        if (request.stock() != null) product.setStock(request.stock());
        if (request.caseMaterial() != null) product.setCaseMaterial(request.caseMaterial());
        if (request.caseDiameter() != null) product.setCaseDiameter(request.caseDiameter());
        if (request.movementType() != null) product.setMovementType(Product.MovementType.valueOf(request.movementType()));
        if (request.waterResistance() != null) product.setWaterResistance(request.waterResistance());
        if (request.crystalType() != null) product.setCrystalType(request.crystalType());
        if (request.dialColor() != null) product.setDialColor(request.dialColor());
        if (request.strapMaterial() != null) product.setStrapMaterial(request.strapMaterial());
        if (request.powerReserve() != null) product.setPowerReserve(request.powerReserve());
        if (request.imageUrl() != null) product.setImageUrl(request.imageUrl());
        if (request.galleryUrls() != null) product.setGalleryUrls(request.galleryUrls());
        if (request.featured() != null) product.setFeatured(request.featured());
        if (request.active() != null) product.setActive(request.active());

        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public boolean reserveStock(UUID productId, int quantity) {
        return productRepository.reserveStock(productId, quantity) > 0;
    }

    @Transactional
    public boolean releaseStock(UUID productId, int quantity) {
        return productRepository.releaseStock(productId, quantity) > 0;
    }

    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(this::toBrandResponse)
                .toList();
    }

    public BrandResponse getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found: " + id));
        return toBrandResponse(brand);
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        return toCategoryResponse(category);
    }

    private Specification<Product> buildSpecification(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (criteria.brandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), criteria.brandId()));
            }
            if (criteria.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), criteria.categoryId()));
            }
            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }
            if (criteria.movementType() != null) {
                predicates.add(cb.equal(root.get("movementType"),
                        Product.MovementType.valueOf(criteria.movementType())));
            }
            if (criteria.query() != null && !criteria.query().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + criteria.query().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ProductResponse toProductResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getReferenceNumber(),
                p.getBrand().getId(),
                p.getBrand().getName(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getPrice(),
                p.getCurrency(),
                p.getStock(),
                p.getCaseMaterial(),
                p.getCaseDiameter(),
                p.getMovementType().name(),
                p.getWaterResistance(),
                p.getCrystalType(),
                p.getDialColor(),
                p.getStrapMaterial(),
                p.getPowerReserve(),
                p.getImageUrl(),
                p.getGalleryUrls(),
                p.getFeatured(),
                p.getActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private BrandResponse toBrandResponse(Brand b) {
        return new BrandResponse(b.getId(), b.getName(), b.getDescription(),
                b.getLogoUrl(), b.getCountry(), b.getCreatedAt());
    }

    private CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(),
                c.getSlug(), c.getCreatedAt());
    }
}
