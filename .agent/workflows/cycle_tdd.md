---
description: Cycle TDD
---

# Workflow: Cycle TDD

## Step 1: Requirement Analysis
Define the feature or bug fix requirements in the context of Orasaka standards.

## Step 2: JUnit 5 Test Case
Write a failing test case in `src/test/java` that defines the expected behavior.

## Step 3: Implementation
Implement the minimal code required to make the test pass.

## Step 4: Verification
Run `mvn test`. Ensure 100% pass rate. No logic is merged without verification.
