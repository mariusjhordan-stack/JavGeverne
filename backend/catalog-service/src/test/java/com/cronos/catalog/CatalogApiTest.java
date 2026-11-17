package com.cronos.catalog;

import com.cronos.catalog.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class CatalogApiTest {

    static final String TOKEN = "test-internal-token-0123456789abcdef0123";
    static final String SUBMARINER = "a1b2c3d4-0001-4000-8000-000000000001";
    static final String DAYTONA = "a1b2c3d4-0002-4000-8000-000000000002";
    static final String SPEEDMASTER = "a1b2c3d4-0003-4000-8000-000000000003";
    static final String SEAMASTER = "a1b2c3d4-0004-4000-8000-000000000004";
    static final String CARRERA = "a1b2c3d4-0005-4000-8000-000000000005";
    static final String INACTIVE = "a1b2c3d4-0006-4000-8000-000000000006";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository products;

    MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, String role) {
        return b.header("X-Internal-Token", TOKEN).header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Email", role.toLowerCase() + "@cronos.test").header("X-User-Role", role);
    }

    MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder b, UUID id) {
        return b.header("X-Internal-Token", TOKEN).header("X-User-Id", id.toString())
                .header("X-User-Email", id + "@cronos.test").header("X-User-Role", "USER");
    }

    MockHttpServletRequestBuilder service(MockHttpServletRequestBuilder b) {
        return b.header("X-Internal-Token", TOKEN).contentType(MediaType.APPLICATION_JSON);
    }

    String reserveBody(UUID orderId, String productId, int quantity) {
        return "{\"orderId\":\"" + orderId + "\",\"items\":[{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}]}";
    }

    int stock(String id) {
        return products.currentStock(UUID.fromString(id));
    }

    // ---------- public catalogue ----------

    @Test
    void anyoneCanBrowseActiveProducts() throws Exception {
        mvc.perform(get("/api/products").param("size", "48"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Inactive Watch"))))
                .andExpect(jsonPath("$.content[0].brandSlug").exists());
        mvc.perform(get("/api/products/" + SUBMARINER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.brandName").value("Rolex"));
        mvc.perform(get("/api/products/" + INACTIVE)).andExpect(status().isNotFound());
        mvc.perform(get("/api/brands")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/brands/tag-heuer")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("TAG Heuer"));
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(get("/api/products/featured")).andExpect(status().isOk());
    }

    @Test
    void searchMatchesNameBrandAndReference() throws Exception {
        mvc.perform(get("/api/products").param("q", "daytona")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/products").param("q", "ROLEX"))
                .andExpect(jsonPath("$.content[*].brandName", everyItem(is("Rolex"))))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/products").param("q", "126610")).andExpect(jsonPath("$.content[0].name").value("Submariner Date"));
        mvc.perform(get("/api/products").param("q", "%")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void sortingAndFilters() throws Exception {
        mvc.perform(get("/api/products").param("sort", "price_asc").param("brandId", "1"))
                .andExpect(jsonPath("$.content[0].name").value("Submariner Date"));
        mvc.perform(get("/api/products").param("sort", "price_desc"))
                .andExpect(jsonPath("$.content[0].name").value("Daytona"));
        mvc.perform(get("/api/products").param("brandId", "1")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/products").param("sort", "passwordHash")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("size", "100000")).andExpect(jsonPath("$.size").value(48));
    }

    @Test
    void errorsDoNotLeakInternals() throws Exception {
        mvc.perform(get("/api/products/not-a-uuid")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request"));
    }

    // ---------- authorization ----------

    @Test
    void forgedRoleHeaderWithoutInternalTokenIsIgnored() throws Exception {
        mvc.perform(post("/api/admin/products").header("X-User-Role", "ADMIN").header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/products").header("X-Internal-Token", "wrong-token-wrong-token-wrong-token-xx")
                        .header("X-User-Role", "ADMIN").header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void normalUserCannotManageProductsOrStock() throws Exception {
        String body = "{\"name\":\"X\",\"brandId\":1,\"categoryId\":1,\"price\":10}";
        mvc.perform(as(post("/api/admin/products"), "USER").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(as(put("/api/admin/products/" + DAYTONA), "USER").contentType(MediaType.APPLICATION_JSON).content("{\"price\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(as(post("/api/admin/products/" + DAYTONA + "/stock-adjustments"), "USER")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":-3}"))
                .andExpect(status().isForbidden());
        mvc.perform(as(get("/api/admin/inventory/summary"), "USER")).andExpect(status().isForbidden());
        assertThat(stock(DAYTONA)).isEqualTo(3);
    }

    @Test
    void internalStockApiIsClosedToUsersAndAdmins() throws Exception {
        String body = reserveBody(UUID.randomUUID(), DAYTONA, 1);
        mvc.perform(post("/internal/stock/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/stock/reservations").header("X-User-Role", "ADMIN")
                        .header("X-User-Id", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        // The old public stock endpoint no longer exists.
        mvc.perform(as(patch("/api/products/" + DAYTONA + "/stock"), "ADMIN").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"reserve\",\"quantity\":1}"))
                .andExpect(status().is4xxClientError());
        assertThat(stock(DAYTONA)).isEqualTo(3);
    }

    @Test
    void adminManagesProductsAndInventory() throws Exception {
        String created = mvc.perform(as(post("/api/admin/products"), "ADMIN").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin Watch\",\"referenceNumber\":\"ADM-1\",\"brandId\":2,\"categoryId\":1,\"price\":1200.50,\"stock\":4,\"gallery\":[\"/images/a.webp\",\"/images/b.webp\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gallery", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(created).get("id").asText();

        mvc.perform(as(put("/api/admin/products/" + id), "ADMIN").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":1300.00,\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(1300.00)).andExpect(jsonPath("$.stock").value(4));
        mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
        mvc.perform(as(get("/api/admin/products/" + id), "ADMIN")).andExpect(status().isOk());

        mvc.perform(as(post("/api/admin/products/" + id + "/stock-adjustments"), "ADMIN").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":6,\"note\":\"Delivery\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(10));
        mvc.perform(as(post("/api/admin/products/" + id + "/stock-adjustments"), "ADMIN").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":-11}"))
                .andExpect(status().isConflict());
        mvc.perform(as(get("/api/admin/inventory/movements").param("productId", id), "ADMIN"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].reason").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.content[0].actor").value("admin@cronos.test"));
        mvc.perform(as(get("/api/admin/inventory/summary"), "ADMIN")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStock[*].name", hasItem("Daytona")));

        mvc.perform(as(post("/api/admin/products"), "ADMIN").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"brandId\":2,\"categoryId\":1,\"price\":-5}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    // ---------- stock reservations (contract used by order-service) ----------

    @Test
    void reserveReturnsOfficialPrice_thenReleaseIsIdempotent() throws Exception {
        UUID order = UUID.randomUUID();
        int before = stock(CARRERA);
        mvc.perform(service(post("/internal/stock/reservations")).content(reserveBody(order, CARRERA, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(5650.00))
                .andExpect(jsonPath("$.items[0].name").value("Carrera Chronograph"))
                .andExpect(jsonPath("$.total").value(11300.00))
                .andExpect(jsonPath("$.currency").value("USD"));
        assertThat(stock(CARRERA)).isEqualTo(before - 2);

        mvc.perform(service(post("/internal/stock/reservations")).content(reserveBody(order, CARRERA, 2))).andExpect(status().isOk());
        assertThat(stock(CARRERA)).isEqualTo(before - 2);
        mvc.perform(service(post("/internal/stock/reservations")).content(reserveBody(order, CARRERA, 3))).andExpect(status().isConflict());

        mvc.perform(service(post("/internal/stock/reservations/" + order + "/release"))).andExpect(jsonPath("$.released").value(1));
        mvc.perform(service(post("/internal/stock/reservations/" + order + "/release"))).andExpect(jsonPath("$.released").value(0));
        assertThat(stock(CARRERA)).isEqualTo(before);
        mvc.perform(service(post("/internal/stock/reservations/" + order + "/confirm"))).andExpect(status().isConflict());
    }

    @Test
    void confirmKeepsStockSold() throws Exception {
        UUID order = UUID.randomUUID();
        int before = stock(SPEEDMASTER);
        mvc.perform(service(post("/internal/stock/reservations")).content(reserveBody(order, SPEEDMASTER, 1))).andExpect(status().isOk());
        mvc.perform(service(post("/internal/stock/reservations/" + order + "/confirm"))).andExpect(status().isOk());
        mvc.perform(service(post("/internal/stock/reservations/" + order + "/confirm"))).andExpect(status().isOk());
        assertThat(stock(SPEEDMASTER)).isEqualTo(before - 1);
        mvc.perform(service(post("/internal/stock/reservations/" + UUID.randomUUID() + "/confirm"))).andExpect(status().isNotFound());
    }

    @Test
    void invalidQuantitiesAndUnavailableProductsAreRejected() throws Exception {
        int before = stock(SUBMARINER);
        for (int quantity : new int[]{0, -1, -999, 100}) {
            mvc.perform(service(post("/internal/stock/reservations")).content(reserveBody(UUID.randomUUID(), SUBMARINER, quantity)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(service(post("/internal/stock/reservations")).content(reserveBody(UUID.randomUUID(), SUBMARINER, before + 1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        mvc.perform(service(post("/internal/stock/reservations")).content(reserveBody(UUID.randomUUID(), INACTIVE, 1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PRODUCT_UNAVAILABLE"));
        mvc.perform(service(post("/internal/stock/reservations")).content("{\"quantity\":-1}")).andExpect(status().isBadRequest());
        assertThat(stock(SUBMARINER)).isEqualTo(before);
    }

    @Test
    void multiLineReservationIsAllOrNothing() throws Exception {
        int subBefore = stock(SUBMARINER);
        String body = "{\"orderId\":\"" + UUID.randomUUID() + "\",\"items\":[{\"productId\":\"" + SUBMARINER + "\",\"quantity\":1},"
                + "{\"productId\":\"" + DAYTONA + "\",\"quantity\":50}]}";
        mvc.perform(service(post("/internal/stock/reservations")).content(body)).andExpect(status().isConflict());
        assertThat(stock(SUBMARINER)).isEqualTo(subBefore);
        assertThat(stock(DAYTONA)).isEqualTo(3);
    }

    @Test
    void concurrentOrdersNeverOversell() throws Exception {
        int available = stock(SEAMASTER);
        int buyers = available + 15;
        ExecutorService pool = Executors.newFixedThreadPool(12);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            Callable<Integer> task = () -> mvc.perform(service(post("/internal/stock/reservations"))
                    .content(reserveBody(UUID.randomUUID(), SEAMASTER, 1))).andReturn().getResponse().getStatus();
            results.add(pool.submit(task));
        }
        int ok = 0;
        int conflict = 0;
        for (Future<Integer> f : results) {
            int status = f.get();
            if (status == 200) ok++;
            else if (status == 409) conflict++;
        }
        pool.shutdown();
        assertThat(ok).isEqualTo(available);
        assertThat(conflict).isEqualTo(buyers - available);
        assertThat(stock(SEAMASTER)).isZero();
    }

    // ---------- favourites ----------

    @Test
    void favouritesArePrivatePerUser() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        mvc.perform(get("/api/favorites")).andExpect(status().isUnauthorized());
        mvc.perform(asUser(put("/api/favorites/" + SUBMARINER), alice)).andExpect(status().isOk());
        mvc.perform(asUser(put("/api/favorites/" + SUBMARINER), alice)).andExpect(status().isOk());
        mvc.perform(as(put("/api/favorites/" + UUID.randomUUID()), "USER")).andExpect(status().isNotFound());
        mvc.perform(asUser(get("/api/favorites"), alice))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(SUBMARINER));
        mvc.perform(asUser(get("/api/favorites"), bob)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(asUser(delete("/api/favorites/" + SUBMARINER), alice)).andExpect(status().isNoContent());
        mvc.perform(asUser(get("/api/favorites"), alice)).andExpect(jsonPath("$", hasSize(0)));
    }
}
