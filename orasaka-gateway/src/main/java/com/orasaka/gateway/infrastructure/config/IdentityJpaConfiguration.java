package com.orasaka.gateway.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA infrastructure configuration for the Orasaka gateway.
 *
 * <p>Enables cross-module entity scanning and repository activation for all {@code
 * com.orasaka.*.entity} and {@code com.orasaka.*.repository} packages, allowing the gateway to
 * access identity, tools, and core entities in a single persistence context.
 *
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.orasaka") // ◄ Scanne tout le monorepo pour les Repositories
@EntityScan(basePackages = "com.orasaka")
public class IdentityJpaConfiguration {}
