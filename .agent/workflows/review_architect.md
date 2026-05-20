---
description: Architectural Review Protocol
---

# Orasaka Architectural Review Protocol

## Documentation Mandate (Java 21)
- **Docstring Requirement**: Every Class, Interface, Record, and Public/Protected Method MUST have a comprehensive Javadoc.
- **Style Guide**: Follow Google Java Style Guide.
- **Mandatory Tags**:
    - `@param`: Detailed description of inputs.
    - `@return`: Expected output and its significance.
    - `@throws`: Documentation of custom `OrasakaException` or standard exceptions.
    - `@see`: Links to related Orasaka abstractions or Spring AI core components.
- **Virtual Thread Safety**: Explicitly document the non-blocking nature of methods leveraging Virtual Threads.

## Review Gates
1. **Domain Purity**: Ensure zero `spring-boot-starter` dependencies in `cors`.
2. **Dependency Audit**: Verify use of Spring AI BOM (1.1.6).
3. **Java 21 Features**: Ensure usage of Records, Pattern Matching, and Virtual Threads where applicable.
4. **Documentation Accuracy**: If code is "naked" (missing Javadocs), the review FAILs.

## Self-Correction Protocol
If Javadocs are missing:
1. Halt execution.
2. Trigger "Documentation Enrichment" task.
3. Re-generate code with full documentation before proceeding.

## Post-Generation Reporting
After each task, provide a "Code Summary":
- **Component Type**: (e.g., Abstract Class, Functional Interface).
- **Responsibility**: A one-sentence summary of the component's role.
- **Virtual Thread Safety**: Confirmation of non-blocking behavior.
