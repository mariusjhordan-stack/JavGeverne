package com.cronos.catalog.favorite;

import com.cronos.catalog.dto.ProductResponse;
import com.cronos.catalog.repository.ProductRepository;
import com.cronos.catalog.security.ApiException;
import com.cronos.catalog.security.RequestUser;
import com.cronos.catalog.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private static final int MAX_FAVORITES = 200;

    private final FavoriteRepository favorites;
    private final ProductRepository products;
    private final CatalogService catalog;

    public FavoriteController(FavoriteRepository favorites, ProductRepository products, CatalogService catalog) {
        this.favorites = favorites;
        this.products = products;
        this.catalog = catalog;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ProductResponse> list(@AuthenticationPrincipal RequestUser user) {
        return favorites.findProductIds(user.id()).stream()
                .map(id -> products.findWithBrandAndCategoryById(id).filter(p -> Boolean.TRUE.equals(p.getActive())).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(catalog::toResponse).toList();
    }

    @PutMapping("/{productId}")
    @Transactional
    public Map<String, Object> add(@AuthenticationPrincipal RequestUser user, @PathVariable UUID productId) {
        if (!products.existsById(productId)) {
            throw ApiException.notFound("Product");
        }
        Favorite.Key key = new Favorite.Key(user.id(), productId);
        if (!favorites.existsById(key)) {
            if (favorites.findProductIds(user.id()).size() >= MAX_FAVORITES) {
                throw ApiException.conflict("TOO_MANY_FAVORITES", "Favourites list is full");
            }
            favorites.save(new Favorite(user.id(), productId));
        }
        return Map.of("productId", productId, "favorite", true);
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public ResponseEntity<Void> remove(@AuthenticationPrincipal RequestUser user, @PathVariable UUID productId) {
        favorites.remove(user.id(), productId);
        return ResponseEntity.noContent().build();
    }
}
