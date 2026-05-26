import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    files: ["**/*.{js,jsx,mjs,ts,tsx,mts,cts}"],
    rules: {
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
      "react-hooks/exhaustive-deps": "error",
      "@next/next/no-img-element": "error",
      "no-restricted-syntax": [
        "error",
        {
          selector: "TSEnumDeclaration",
          message:
            "Traditional TypeScript enums are strictly banned in Orasaka. Use 'export const MyValues = [...] as const' paired with 'export type MyType = typeof MyValues[number]' to maintain pure, tree-shakable, structural Type-Safe compliance.",
        },
        {
          selector:
            "JSXElement[openingElement.name.name='div'] > JSXElement[openingElement.name.name='div'] > JSXElement[openingElement.name.name='div']",
          message:
            "Deeply nested generic <div> structures are banned in Orasaka. Use semantic HTML5 elements (<main>, <section>, <article>, <header>, <footer>).",
        },
        {
          selector:
            "CallExpression[callee.type='MemberExpression'][callee.property.name=/^set(Date|Hours|Minutes|Seconds|Milliseconds|Month|FullYear|UTCDate|UTCHours|UTCMinutes|UTCSeconds|UTCMilliseconds|UTCMonth|UTCFullYear|Time)$/]",
          message:
            "Mutating native JavaScript Date instances via set* methods is strictly forbidden in Orasaka. Use date-fns instead.",
        },
        {
          selector:
            "ExportNamedDeclaration > :matches(TSInterfaceDeclaration, TSTypeAliasDeclaration, ClassDeclaration, FunctionDeclaration)[id.name=/^Orasaka/]",
          message:
            "[ERR-104] Orasaka prefix is banned on internal types. The module path already establishes ownership. Use clean domain names (e.g., 'Settings' not 'OrasakaSettings').",
        },
      ],
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
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
