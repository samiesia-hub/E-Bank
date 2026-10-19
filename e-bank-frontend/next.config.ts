import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* cacheComponents (Next 16 PPR) disabled: classic App Router behaviour.
     Our portal pages use client-side TanStack Query + httpOnly session cookie,
     and dynamic [id] routes — simpler and compatible with `export const dynamic`. */
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
