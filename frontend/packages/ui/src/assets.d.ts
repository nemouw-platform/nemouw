// Image imports resolve to Next.js static image data when bundled by the apps
// (they list @workspace/ui in transpilePackages). This lets the package
// typecheck on its own, without Next's global types.
declare module "*.png" {
  const image: { src: string; width: number; height: number }
  export default image
}
