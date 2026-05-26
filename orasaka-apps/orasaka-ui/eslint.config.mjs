import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    files: ["**/*.{js,jsx,mjs,ts,tsx,mts,cts}"],
    rules: {
      // --- SECURITY AND STRICT TYPING RULES ---
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
      "react-hooks/exhaustive-deps": "error",
      "@next/next/no-img-element": "error",
      
      // --- PRODUCTION HYPER-GOVERNANCE ---
      "no-console": ["error", { allow: ["warn", "error"] }], // Disallow raw debug logs in production pipelines
      "no-debugger": "error", // Block left-over development debugging breakpoints
      
      "no-restricted-syntax": [
        "error",
        // 1. Orasaka Invariant: Banning traditional TS Enums
        {
          selector: "TSEnumDeclaration",
          message:
            "Traditional TypeScript enums are strictly banned in Orasaka. Use 'export const MyValues = [...] as const' paired with 'export type MyType = typeof MyValues[number]' to maintain pure, tree-shakable, structural Type-Safe compliance.",
        },
        
        // 2. Krizaka UI Invariant: Banning generic deep HTML structures (Div Soup)
        {
          selector:
            "JSXElement[openingElement.name.name='div'] > JSXElement[openingElement.name.name='div'] > JSXElement[openingElement.name.name='div']",
          message:
            "Deeply nested generic <div> structures are banned in Orasaka. Use semantic HTML5 elements (<main>, <section>, <article>, <header>, <footer>) to ensure structural accessibility.",
        },
        
        // 3. Temporal Invariant: Forbidding mutation of native Date instances
        {
          selector:
            "CallExpression[callee.type='MemberExpression'][callee.property.name=/^set(Date|Hours|Minutes|Seconds|Milliseconds|Month|FullYear|UTCDate|UTCHours|UTCMinutes|UTCSeconds|UTCMilliseconds|UTCMonth|UTCFullYear|Time)$/]",
          message:
            "Mutating native JavaScript Date instances via set* methods is strictly forbidden in Orasaka. Use date-fns instead to preserve immutability.",
        },
        
        // 4. Naming Invariant [ERR-104]: Forbidding redundant prefixes
        {
          selector:
            "ExportNamedDeclaration > :matches(TSInterfaceDeclaration, TSTypeAliasDeclaration, ClassDeclaration, FunctionDeclaration)[id.name=/^Orasaka/]",
          message:
            "[ERR-104] Orasaka prefix is banned on internal types. The module path already establishes ownership. Use clean domain names (e.g., 'Settings' not 'OrasakaSettings').",
        },

        // 5. Tailwind v4 / Style Invariant: Forbidding inline style objects
        {
          selector: "JSXAttribute[name.name='style']",
          message:
            "Inline style objects are strictly forbidden in Orasaka. You must leverage Tailwind v4 CSS utility tokens or custom rules within layout classes inside globals.css to ensure Design System consistency.",
        },

        // 6. Next.js Async / Hydration Invariant: Forbidding raw fetch calls inside useEffect
        {
          selector: "CallExpression[callee.name='useEffect'] CallExpression[callee.property.name='fetch']",
          message:
            "Data fetching inside useEffect hooks is forbidden in Orasaka. Use Next.js Server Components, Server Actions, or dedicated cache-aware SWR/TanStack Query pipelines to manage state hydration cleanly.",
        }
      ],
      
      // --- IMPORTS AND ARCHITECTURAL BOUNDARIES ---
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: ["../*", "../**"],
              message:
                "Relative imports from parent directories are strictly banned in Orasaka. You must leverage the absolute path alias '@/*' (pointing to './src/*') to maintain clean module boundaries and refactoring flexibility.",
            },
          ],
        },
      ],
    },
  },
  // Align globally ignored build directories
  globalIgnores([
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    "coverage/**",
    "node_modules/**",
  ]),
]);

export default eslintConfig;