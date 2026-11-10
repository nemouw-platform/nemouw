// One file per backend area (auth, specialists, ...), each a factory taking an ApiClient
export { createAuthApi, type AuthApi } from "./auth"
export type { CallOptions } from "./types"
