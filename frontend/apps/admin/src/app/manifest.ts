import type { MetadataRoute } from "next"

// Served at /manifest.webmanifest and linked in <head> automatically.
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Nemouw Admin",
    short_name: "Nemouw Admin",
    start_url: "/",
    display: "standalone",
    theme_color: "#ffffff",
    background_color: "#ffffff",
    icons: [
      {
        src: "/web-app-manifest-192x192.png",
        sizes: "192x192",
        type: "image/png",
        purpose: "maskable",
      },
      {
        src: "/web-app-manifest-512x512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "maskable",
      },
    ],
  }
}
