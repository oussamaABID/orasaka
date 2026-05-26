/** @type {import('dependency-cruiser').IConfiguration} */
module.exports = {
  forbidden: [
    {
      name: "no-circular",
      severity: "error",
      comment:
        "Circular dependencies are banned across the entire source tree. They increase coupling and make refactoring hazardous.",
      from: {},
      to: {
        circular: true,
      },
    },
    {
      name: "no-feature-cross-import",
      severity: "warn",
      comment:
        "Feature modules must not import from other feature modules directly. Shared logic must be extracted to src/core/ or src/components/.",
      from: {
        path: "^src/features/([^/]+)/",
      },
      to: {
        path: "^src/features/([^/]+)/",
        pathNot: "^src/features/$1/",
      },
    },
    {
      name: "no-orphan-modules",
      severity: "warn",
      comment:
        "Modules that are not imported by any other module are likely dead code.",
      from: {
        orphan: true,
        pathNot: [
          "(^|/)\\.[^/]+", // dot files
          "\\.d\\.ts$", // type declaration files
          "(^|/)tsconfig\\.json$",
          "src/app/", // Next.js app router pages are entry points
          "__tests__/", // test files
        ],
      },
      to: {},
    },
    {
      name: "no-components-import-features",
      severity: "warn",
      comment:
        "Shared UI components must not import from feature modules. Only features import components, never the reverse.",
      from: {
        path: "^src/components/",
      },
      to: {
        path: "^src/features/",
      },
    },
    {
      name: "no-types-import-implementation",
      severity: "error",
      comment:
        "Type definition files must not import from feature or component implementations. Types are pure structural contracts.",
      from: {
        path: "^src/types/",
      },
      to: {
        path: "^src/(features|components)/",
      },
    },
    {
      name: "no-direct-graphql-access",
      severity: "error",
      comment:
        "Features and components must not import directly from src/core/graphql or call GraphQL endpoints bypassing the API client abstraction layer.",
      from: {
        path: "^src/(features|components)/",
      },
      to: {
        path: "^src/core/graphql/",
      },
    },
    {
      name: "no-leaking-feature-constants",
      severity: "error",
      comment:
        "Transversal constants and configuration defaults must not live inside a specific feature. Relocate them to src/constants/ to prevent cross-feature coupling.",
      from: {
        path: "^src/",
        pathNot: "^src/features/settings/",
      },
      to: {
        path: "^src/features/settings/constants/",
      },
    },
  ],
  options: {
    doNotFollow: {
      path: "node_modules",
    },
    tsPreCompilationDeps: true,
    tsConfig: {
      fileName: "tsconfig.json",
    },
    enhancedResolveOptions: {
      exportsFields: ["exports"],
      conditionNames: ["import", "require", "node", "default"],
    },
    reporterOptions: {
      dot: {
        collapsePattern: "node_modules/(@[^/]+/[^/]+|[^/]+)",
      },
    },
  },
};
