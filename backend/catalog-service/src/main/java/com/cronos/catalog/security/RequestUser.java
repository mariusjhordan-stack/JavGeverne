package com.cronos.catalog.security;

import java.util.UUID;

/** End user on whose behalf the gateway forwarded the request. */
public record RequestUser(UUID id, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
