# Orasaka Functional Purity & Domain Record Invariants

This document outlines the strict functional programming and domain purity rules enforced across all Java modules (`orasaka-core`, `orasaka-identity`, `orasaka-gateway`) in the Orasaka ecosystem.

---

## 🏛️ 1. The Anemic Service & Rich Domain Record Paradigm

To leverage high-throughput execution under Java 21 Virtual Threads and prevent concurrency bottlenecks, we strictly decouple business orchestration from state validation and data-assembly invariants.

### A. Core Axioms
* **Anemic Services**: Service layers (such as `IdentityService` or `AbstractOrasakaEngine`) must act strictly as stateless orchestrators. They are prohibited from:
  - Checking parameters for nullability or assigning default/fallback values procedural style (e.g. `String lang = language != null ? language : "en";`).
  - Checking collection sizes or defensively copying lists inside service methods.
  - Direct type-casting and manual object mapping inside execution flows.
* **Rich Domain Records**: Every state constraint, validation guard, default parameter value, and collection defensive copy MUST reside within the **Compact Constructor** of the corresponding Java 21 `record` (e.g., `User`, `OrasakaChatRequest`, `PromptContext`).

---

## 🎨 2. Functional Purity Rules

### A. Universal Record Invariant Validation (`[ERR-103]`)
All domain records must be self-validating and immutable at construction time.

* **Incorrect (Procedural Leakage inside Service)**:
```java
public class IdentityService {
    public User registerUser(String username, String email, String language) {
        String finalLanguage = (language == null || language.isBlank()) ? "en" : language;
        // procedural checks...
        return new User(UUID.randomUUID(), username, email, true, Set.of("ROLE_USER"), Map.of(), List.of());
    }
}
```

* **Correct (Self-Validating Domain Record)**:
```java
public record User(
    UUID id,
    String username,
    String email,
    boolean active,
    Set<String> authorities,
    Map<String, Object> preferences,
    List<InterceptionEntity> activeInterceptions
) {
    public User {
        Objects.requireNonNull(id, "ID must not be null");
        Objects.requireNonNull(username, "Username must not be null");
        Objects.requireNonNull(email, "Email must not be null");
        authorities = Set.copyOf(authorities != null ? authorities : Set.of());
        preferences = Map.copyOf(preferences != null ? preferences : Map.of());
        activeInterceptions = List.copyOf(activeInterceptions != null ? activeInterceptions : List.of());
    }
}
```

### B. Functional Pipeline Reduction (`[ERR-104]`)
Procedural loops (`for`, `while`) and local variable null mutations (initializing a variable to `null` to mutate it later inside a conditional block) are strictly banned.

* **Incorrect (Procedural Mutation)**:
```java
String promptText = request.prompt();
for (OrasakaContextInterceptor interceptor : interceptors) {
    promptText = interceptor.preProcess(request, promptText, messages, options);
}
```

* **Correct (Functional Stream Reduction)**:
```java
String refinedPrompt = interceptors.stream()
    .reduce(
        request.prompt(),
        (text, interceptor) -> interceptor.preProcess(request, text, messages, options),
        (t1, t2) -> t1
    );
```

### C. Pattern Matching over Sealed Hierarchies (`[ERR-107]`)
To preserve absolute type-safety without unsafe casting, evaluating polymorphic components must be done using Java 21 pattern-matching `switch` expressions:

* **Correct (Pattern-Matching Switch)**:
```java
public String nodeStateType(NodeState state) {
    return switch (state) {
        case Active a -> "ACTIVE";
        case Locked l -> "LOCKED";
        case Invisible i -> "INVISIBLE";
    };
}
```

---

## 🚀 3. Quality & Formatting Verification

To guarantee adherence to the functional purity standard, all modifications must satisfy the static checking rules:
* **`[ERR-109]`**: Strict Data Component Naming & Record Conventions (carriers must be `record` types).
* **`[ERR-110]`**: Collapse Package Architecture & Encapsulation Boundary (utility beans and orchestrator implementations must remain package-private).

---

## 🎙️ 4. CinePulse AI Multi-Modal Ingestion Guidelines

To handle film assets within the ingestion pipeline, tools exposing vision or audio extraction processing must conform to standard contract structures.

### A. Vision Poster Ingestion (`AnalyzePosterRequest`)
All movie poster classification and vision-based analyses must accept a payload containing a base64 encoded image string alongside structured contextual prompts:
* **Contract Schema**:
  - `posterBase64`: The raw string of the image file encoded in Base64 format. Must be validated non-null and non-blank.
  - `prompt`: Direct context instructions for the vision execution model. Falls back to a generic analysis description if empty.

### B. Audio Extract Compliance (`AnalyzeAudioExtractRequest`)
All film audio tracking, clip inspections, and sound-based verification filters must process local media references:
* **Contract Schema**:
  - `clipPath`: Absolute or relative path to the source audio file. Must be validated non-null and non-blank.
  - `checkType`: Target compliance verification or content checks category (e.g. compliance, loudness checking).

