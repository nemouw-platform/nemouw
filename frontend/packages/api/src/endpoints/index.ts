// One file per backend area (auth, specialists, ...), each a factory taking an ApiClient
export { createAuthApi, type AuthApi } from "./auth"
export { createSpecialistsApi, type SpecialistsApi } from "./specialists"
export type { CallOptions } from "./types"
