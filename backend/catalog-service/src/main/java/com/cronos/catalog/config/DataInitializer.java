package com.cronos.catalog.config;

import com.cronos.catalog.entity.Brand;
import com.cronos.catalog.entity.Category;
import com.cronos.catalog.entity.Product;
import com.cronos.catalog.repository.BrandRepository;
import com.cronos.catalog.repository.CategoryRepository;
import com.cronos.catalog.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile({"dev", "local"})
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DataInitializer(BrandRepository brandRepository, CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (brandRepository.count() > 0) {
            log.info("Catalog data already exists - skipping initialization");
            return;
        }

        Brand rolex = createBrand("Rolex", "Swiss luxury watchmaker founded in 1905", "Switzerland");
        Brand omega = createBrand("Omega", "Swiss luxury watchmaker, part of Swatch Group", "Switzerland");
        Brand patek = createBrand("Patek Philippe", "One of the oldest Swiss watchmakers, founded in 1839", "Switzerland");

        Category sport = createCategory("Sport", "sport", "Watches designed for athletic and outdoor activities");
        Category dress = createCategory("Dress", "dress", "Elegant timepieces for formal occasions");
        Category dive = createCategory("Dive", "dive", "Professional diving watches with water resistance");
        Category pilot = createCategory("Pilot", "pilot", "Aviation-inspired timepieces");

        createProduct("Rolex Submariner Date", "REF-126610LN", rolex, dive,
                new BigDecimal("9100.00"), "The quintessential dive watch",
                "Oystersteel", "41mm", Product.MovementType.AUTOMATIC, "300m",
                "Sapphire", "Black", "Oyster bracelet", "70 hours", true);

        createProduct("Rolex Daytona", "REF-126500LN", rolex, sport,
                new BigDecimal("14550.00"), "The ultimate racing chronograph",
                "Oystersteel", "40mm", Product.MovementType.AUTOMATIC, "100m",
                "Sapphire", "White", "Oyster bracelet", "72 hours", true);

        createProduct("Rolex Datejust 41", "REF-126334", rolex, dress,
                new BigDecimal("10250.00"), "The archetype of the classic watch",
                "Oystersteel & White Gold", "41mm", Product.MovementType.AUTOMATIC, "100m",
                "Sapphire", "Blue", "Jubilee bracelet", "70 hours", false);

        createProduct("Omega Seamaster 300M", "REF-210.30.42", omega, dive,
                new BigDecimal("5550.00"), "A legend in professional diving",
                "Stainless Steel", "42mm", Product.MovementType.AUTOMATIC, "300m",
                "Sapphire", "Blue", "Steel bracelet", "55 hours", true);

        createProduct("Omega Speedmaster Moonwatch", "REF-310.30.42", omega, sport,
                new BigDecimal("6550.00"), "The first watch worn on the Moon",
                "Stainless Steel", "42mm", Product.MovementType.MANUAL, "50m",
                "Hesalite", "Black", "Steel bracelet", "50 hours", true);

        createProduct("Omega Constellation", "REF-131.10.39", omega, dress,
                new BigDecimal("6000.00"), "Timeless elegance since 1952",
                "Stainless Steel", "39mm", Product.MovementType.AUTOMATIC, "50m",
                "Sapphire", "Silver", "Steel bracelet", "55 hours", false);

        createProduct("Patek Philippe Nautilus", "REF-5711/1A", patek, sport,
                new BigDecimal("35000.00"), "The iconic luxury sports watch",
                "Stainless Steel", "40mm", Product.MovementType.AUTOMATIC, "120m",
                "Sapphire", "Blue", "Steel bracelet", "45 hours", true);

        createProduct("Patek Philippe Calatrava", "REF-5227G", patek, dress,
                new BigDecimal("42000.00"), "The essence of the round wristwatch",
                "White Gold", "39mm", Product.MovementType.AUTOMATIC, "30m",
                "Sapphire", "White", "Alligator strap", "45 hours", true);

        createProduct("Patek Philippe Aquanaut", "REF-5167A", patek, sport,
                new BigDecimal("28000.00"), "Modern elegance meets sportiness",
                "Stainless Steel", "40mm", Product.MovementType.AUTOMATIC, "120m",
                "Sapphire", "Black", "Composite strap", "45 hours", false);

        createProduct("Rolex GMT-Master II", "REF-126710BLNR", rolex, pilot,
                new BigDecimal("10900.00"), "The traveler's watch",
                "Oystersteel", "40mm", Product.MovementType.AUTOMATIC, "100m",
                "Sapphire", "Black", "Jubilee bracelet", "70 hours", false);

        createProduct("Omega De Ville Prestige", "REF-424.10.40", omega, dress,
                new BigDecimal("3400.00"), "Classic elegance at its finest",
                "Stainless Steel", "39.5mm", Product.MovementType.AUTOMATIC, "30m",
                "Sapphire", "Silver", "Steel bracelet", "48 hours", false);

        createProduct("Rolex Explorer II", "REF-226570", rolex, sport,
                new BigDecimal("9150.00"), "Built for exploration",
                "Oystersteel", "42mm", Product.MovementType.AUTOMATIC, "100m",
                "Sapphire", "White", "Oyster bracelet", "70 hours", false);

        log.info("Catalog seed data initialized: 3 brands, 4 categories, 12 products");
    }

    private Brand createBrand(String name, String description, String country) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setDescription(description);
        brand.setCountry(country);
        return brandRepository.save(brand);
    }

    private Category createCategory(String name, String slug, String description) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    private void createProduct(String name, String ref, Brand brand, Category category,
                               BigDecimal price, String description, String caseMaterial,
                               String caseDiameter, Product.MovementType movement, String waterResistance,
                               String crystal, String dialColor, String strapMaterial,
                               String powerReserve, boolean featured) {
        Product product = new Product();
        product.setName(name);
        product.setReferenceNumber(ref);
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(price);
        product.setDescription(description);
        product.setCaseMaterial(caseMaterial);
        product.setCaseDiameter(caseDiameter);
        product.setMovementType(movement);
        product.setWaterResistance(waterResistance);
        product.setCrystalType(crystal);
        product.setDialColor(dialColor);
        product.setStrapMaterial(strapMaterial);
        product.setPowerReserve(powerReserve);
        product.setFeatured(featured);
        product.setStock(5);
        product.setActive(true);
        productRepository.save(product);
    }
}
