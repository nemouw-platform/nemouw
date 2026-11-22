import path from "node:path"

import type { NextConfig } from "next"
import createNextIntlPlugin from "next-intl/plugin"

const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts")

// The Docker image runs Next's minimal server (frontend/Dockerfile sets
// NEXT_OUTPUT=standalone). Local builds keep the regular output for
// `next start`, and need no symlink rights on Windows.
const standalone = process.env.NEXT_OUTPUT === "standalone"

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  transpilePackages: ["@workspace/ui", "@workspace/api", "@workspace/i18n"],
  ...(standalone && {
    output: "standalone",
    // Trace from the monorepo root, so the workspace packages are included
    // (`next build` runs in the app's folder)
    outputFileTracingRoot: path.resolve(process.cwd(), "../.."),
  }),
}

export default withNextIntl(nextConfig)
