package com.cronos.order.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/** Service-specific rules for /api/** (admin and internal paths are handled in SecurityConfig). */
public interface ApiAccessRules {

    void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth);
}
