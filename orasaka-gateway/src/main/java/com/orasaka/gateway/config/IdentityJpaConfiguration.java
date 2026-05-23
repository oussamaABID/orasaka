package com.orasaka.gateway.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.orasaka.*.repository")
@EntityScan(basePackages = "com.orasaka.*.entity")
public class IdentityJpaConfiguration {}
