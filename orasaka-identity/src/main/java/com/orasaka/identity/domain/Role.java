package com.orasaka.identity.domain;

/**
 * Domain-driven RBAC hierarchy using Java 21 Sealed Interfaces.
 */
public sealed interface Role permits Role.Admin, Role.Operator, Role.Guest {

    String name();

    record Admin() implements Role {
        @Override public String name() { return "ADMIN"; }
    }

    record Operator() implements Role {
        @Override public String name() { return "OPERATOR"; }
    }

    record Guest() implements Role {
        @Override public String name() { return "GUEST"; }
    }
}
