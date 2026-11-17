package com.cronos.catalog.service;

import com.cronos.catalog.dto.*;
import com.cronos.catalog.entity.Brand;
import com.cronos.catalog.entity.Category;
import com.cronos.catalog.entity.Product;
import com.cronos.catalog.repository.BrandRepository;
import com.cronos.catalog.repository.CategoryRepository;
import com.cronos.catalog.repository.ProductRepository;
import com.cronos.catalog.security.ApiException;
import com.cronos.catalog.stock.StockMovement;
import com.cronos.catalog.stock.StockMovementRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    public static final int MAX_PAGE_SIZE = 48;

    public record ProductFilter(String query, Long brandId, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                String movementType, Boolean featured, boolean includeInactive) {}

    private static final Map<String, Sort> SORTS = Map.of(
            "newest", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")),
            "price_asc", Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id")),
            "price_desc", Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id")),
            "name", Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository movementRepository;
    private final ObjectMapper objectMapper;

    public CatalogService(ProductRepository productRepository, BrandRepository brandRepository,
                          CategoryRepository categoryRepository, StockMovementRepository movementRepository,
                          ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.movementRepository = movementRepository;
        this.objectMapper = objectMapper;
    }

    public Page<ProductResponse> findProducts(ProductFilter filter, String sort, int page, int size) {
        Sort order = SORTS.get(sort == null || sort.isBlank() ? "newest" : sort);
        if (order == null) {
            throw ApiException.badRequest("Unknown sort. Use one of: newest, price_asc, price_desc, name");
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), order);
        return productRepository.findAll(specification(filter), pageable).map(this::toResponse);
    }

    public ProductResponse getActiveProduct(UUID id) {
        Product product = productRepository.findWithBrandAndCategoryById(id)
                .filter(Product::getActive)
                .orElseThrow(() -> ApiException.notFound("Product"));
        return toResponse(product);
    }

    public ProductResponse getProduct(UUID id) {
        return toResponse(productRepository.findWithBrandAndCategoryById(id).orElseThrow(() -> ApiException.notFound("Product")));
    }

    public List<ProductResponse> getFeaturedProducts() {
        return productRepository.findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, String actor) {
        if (request.referenceNumber() != null && !request.referenceNumber().isBlank()
                && productRepository.existsByReferenceNumber(request.referenceNumber())) {
            throw ApiException.conflict("DUPLICATE_REFERENCE", "A product with this reference number already exists");
        }
        Product product = new Product();
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setReferenceNumber(blankToNull(request.referenceNumber()));
        product.setBrand(brand(request.brandId()));
        product.setCategory(category(request.categoryId()));
        product.setPrice(request.price());
        product.setCurrency("USD");
        product.setStock(request.stock() == null ? 0 : request.stock());
        product.setCaseMaterial(request.caseMaterial());
        product.setCaseDiameter(request.caseDiameter());
        product.setMovementType(Product.MovementType.valueOf(request.movementType() == null ? "AUTOMATIC" : request.movementType()));
        product.setWaterResistance(request.waterResistance());
        product.setCrystalType(request.crystalType());
        product.setDialColor(request.dialColor());
        product.setStrapMaterial(request.strapMaterial());
        product.setPowerReserve(request.powerReserve());
        product.setImageUrl(blankToNull(request.imageUrl()));
        product.setGalleryUrls(writeGallery(request.gallery()));
        product.setFeatured(Boolean.TRUE.equals(request.featured()));
        product.setActive(true);
        Product saved = productRepository.save(product);
        if (saved.getStock() > 0) {
            movementRepository.save(new StockMovement(saved.getId(), saved.getStock(), saved.getStock(),
                    StockMovement.Reason.INITIAL, "Product created", actor));
        }
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findWithBrandAndCategoryById(id).orElseThrow(() -> ApiException.notFound("Product"));
        if (request.name() != null) product.setName(request.name().trim());
        if (request.description() != null) product.setDescription(request.description());
        if (request.referenceNumber() != null) product.setReferenceNumber(blankToNull(request.referenceNumber()));
        if (request.brandId() != null) product.setBrand(brand(request.brandId()));
        if (request.categoryId() != null) product.setCategory(category(request.categoryId()));
        if (request.price() != null) product.setPrice(request.price());
        if (request.caseMaterial() != null) product.setCaseMaterial(request.caseMaterial());
        if (request.caseDiameter() != null) product.setCaseDiameter(request.caseDiameter());
        if (request.movementType() != null) product.setMovementType(Product.MovementType.valueOf(request.movementType()));
        if (request.waterResistance() != null) product.setWaterResistance(request.waterResistance());
        if (request.crystalType() != null) product.setCrystalType(request.crystalType());
        if (request.dialColor() != null) product.setDialColor(request.dialColor());
        if (request.strapMaterial() != null) product.setStrapMaterial(request.strapMaterial());
        if (request.powerReserve() != null) product.setPowerReserve(request.powerReserve());
        if (request.imageUrl() != null) product.setImageUrl(blankToNull(request.imageUrl()));
        if (request.gallery() != null) product.setGalleryUrls(writeGallery(request.gallery()));
        if (request.featured() != null) product.setFeatured(request.featured());
        if (request.active() != null) product.setActive(request.active());
        return toResponse(product);
    }

    public List<BrandResponse> getBrands() {
        return brandRepository.findAll(Sort.by("id")).stream().map(this::toBrandResponse).toList();
    }

    public BrandResponse getBrand(String idOrSlug) {
        Brand brand = (idOrSlug.chars().allMatch(Character::isDigit)
                ? brandRepository.findById(Long.parseLong(idOrSlug))
                : brandRepository.findBySlug(idOrSlug))
                .orElseThrow(() -> ApiException.notFound("Brand"));
        return toBrandResponse(brand);
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getSlug(), c.getCreatedAt()))
                .toList();
    }

    private Specification<Product> specification(ProductFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!f.includeInactive()) predicates.add(cb.isTrue(root.get("active")));
            if (f.brandId() != null) predicates.add(cb.equal(root.get("brand").get("id"), f.brandId()));
            if (f.categoryId() != null) predicates.add(cb.equal(root.get("category").get("id"), f.categoryId()));
            if (f.minPrice() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), f.minPrice()));
            if (f.maxPrice() != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), f.maxPrice()));
            if (f.featured() != null) predicates.add(cb.equal(root.get("featured"), f.featured()));
            if (f.movementType() != null && !f.movementType().isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("movementType"), Product.MovementType.valueOf(f.movementType().toUpperCase())));
                } catch (IllegalArgumentException e) {
                    throw ApiException.badRequest("Unknown movement type");
                }
            }
            if (f.query() != null && !f.query().isBlank()) {
                String like = "%" + f.query().trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                var brand = root.join("brand", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like, '\\'),
                        cb.like(cb.lower(brand.get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("referenceNumber")), like, '\\')));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Brand brand(Long id) {
        return brandRepository.findById(id).orElseThrow(() -> ApiException.badRequest("Unknown brand"));
    }

    private Category category(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> ApiException.badRequest("Unknown category"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String writeGallery(List<String> gallery) {
        try {
            return objectMapper.writeValueAsString(gallery == null ? List.of() : gallery);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> readGallery(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getReferenceNumber(),
                p.getBrand().getId(), p.getBrand().getName(), p.getBrand().getSlug(),
                p.getCategory().getId(), p.getCategory().getName(), p.getCategory().getSlug(),
                p.getPrice(), p.getCurrency(), p.getStock(),
                p.getCaseMaterial(), p.getCaseDiameter(), p.getMovementType().name(), p.getWaterResistance(),
                p.getCrystalType(), p.getDialColor(), p.getStrapMaterial(), p.getPowerReserve(),
                p.getImageUrl(), readGallery(p.getGalleryUrls()), p.getFeatured(), p.getActive(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private BrandResponse toBrandResponse(Brand b) {
        long count = productRepository.count((root, query, cb) ->
                cb.and(cb.equal(root.get("brand").get("id"), b.getId()), cb.isTrue(root.get("active"))));
        return new BrandResponse(b.getId(), b.getName(), b.getSlug(), b.getDescription(), b.getStory(),
                b.getImageUrl(), b.getCountry(), count);
    }
}
