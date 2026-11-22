import { config } from "@workspace/eslint-config/base"

/** @type {import("eslint").Linter.Config} */
export default [
  ...config,
  {
    // Generated from the backend's openapi.yaml by `pnpm generate`
    ignores: ["src/generated/**"],
  },
]
