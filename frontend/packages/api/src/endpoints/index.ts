// One file per backend area (auth, specialists, ...), each a factory taking an ApiClient
export { createAuthApi, type AuthApi } from "./auth"
export { createConditionsApi, type ConditionsApi } from "./conditions"
export { createCountriesApi, type CountriesApi } from "./countries"
export { createCredentialsApi, type CredentialsApi } from "./credentials"
export { createLocationsApi, type LocationsApi } from "./locations"
export { createSpecialistsApi, type SpecialistsApi } from "./specialists"
export { createSyndicatesApi, type SyndicatesApi } from "./syndicates"
export type { CallOptions } from "./types"
