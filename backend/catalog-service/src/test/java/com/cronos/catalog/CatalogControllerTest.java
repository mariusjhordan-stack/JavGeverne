package com.cronos.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listProducts_returnsPaginatedActiveProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.content[*].active", everyItem(is(true))));
    }

    @Test
    void listProducts_respectsPagination() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(5)));
    }

    @Test
    void getProductById_returnsProduct() throws Exception {
        mockMvc.perform(get("/api/products/{id}", "a1b2c3d4-0001-4000-8000-000000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Submariner Date"))
                .andExpect(jsonPath("$.brandName").value("Rolex"))
                .andExpect(jsonPath("$.categoryName").value("Dive"))
                .andExpect(jsonPath("$.price").value(9150.00));
    }

    @Test
    void getProductById_notFound() throws Exception {
        mockMvc.perform(get("/api/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }

    @Test
    void getFeaturedProducts_returnsOnlyFeatured() throws Exception {
        mockMvc.perform(get("/api/products/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].featured", everyItem(is(true))));
    }

    @Test
    void searchProducts_byName() throws Exception {
        mockMvc.perform(get("/api/products/search").param("q", "submariner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Submariner Date"));
    }

    @Test
    void searchProducts_caseInsensitive() throws Exception {
        mockMvc.perform(get("/api/products/search").param("q", "DAYTONA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Daytona"));
    }

    @Test
    void filterByBrand_viaQueryParam() throws Exception {
        mockMvc.perform(get("/api/products").param("brandId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content[*].brandName", everyItem(is("Rolex"))));
    }

    @Test
    void filterByCategory_viaQueryParam() throws Exception {
        mockMvc.perform(get("/api/products").param("categoryId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].categoryName", everyItem(is("Dive"))));
    }

    @Test
    void getProductsByBrand_endpoint() throws Exception {
        mockMvc.perform(get("/api/products/brand/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].brandName", everyItem(is("Omega"))));
    }

    @Test
    void getProductsByCategory_endpoint() throws Exception {
        mockMvc.perform(get("/api/products/category/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].categoryName", everyItem(is("Chronograph"))));
    }

    @Test
    void getAllBrands() throws Exception {
        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", hasItems("Rolex", "Omega", "TAG Heuer")));
    }

    @Test
    void getBrandById() throws Exception {
        mockMvc.perform(get("/api/brands/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rolex"))
                .andExpect(jsonPath("$.country").value("Switzerland"));
    }

    @Test
    void getAllCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].name", hasItems("Sport", "Dress", "Dive", "Chronograph")));
    }

    @Test
    void getCategoryById() throws Exception {
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sport"))
                .andExpect(jsonPath("$.slug").value("sport"));
    }

    @Test
    void createProduct_forbiddenWithoutAdminRole() throws Exception {
        String json = """
                {
                  "name": "Test Watch",
                  "description": "Test",
                  "referenceNumber": "TEST-001",
                  "brandId": 1,
                  "categoryId": 1,
                  "price": 1000.00,
                  "movementType": "AUTOMATIC"
                }
                """;
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_succeeds_withAdminRole() throws Exception {
        String json = """
                {
                  "name": "New Luxury Watch",
                  "description": "A new test watch",
                  "referenceNumber": "NEW-001",
                  "brandId": 1,
                  "categoryId": 1,
                  "price": 7500.00,
                  "movementType": "AUTOMATIC"
                }
                """;
        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Luxury Watch"))
                .andExpect(jsonPath("$.brandName").value("Rolex"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void inactiveProducts_excluded_fromListing() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Inactive Watch"))));
    }

    @Test
    void filterByPrice_range() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "6000")
                        .param("maxPrice", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].price", everyItem(
                        allOf(greaterThanOrEqualTo(6000.0), lessThanOrEqualTo(10000.0)))));
    }
}
