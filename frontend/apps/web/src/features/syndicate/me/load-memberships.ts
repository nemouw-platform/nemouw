import { isApiError } from "@workspace/api"

import { findMember, type Membership } from "@/features/syndicate/me/membership"
import { syndicatesApi } from "@/lib/api"

/**
 * The syndicates the user is on the staff of.
 *
 * TEMPORARY: the backend cannot tell yet (the session holds only the role),
 * so this reads each syndicate's staff list, which answers 403 to anyone not
 * on it. One request per syndicate. Replace the body with the backend's
 * `GET /syndicates/mine` once it exists; nothing else needs to change.
 */
export async function findMemberships(
  userId: string,
  signal?: AbortSignal
): Promise<Membership[]> {
  const syndicates = await syndicatesApi.list({ signal })
  const memberships: Membership[] = []
  for (const syndicate of syndicates) {
    if (!syndicate.id) continue
    try {
      const staff = await syndicatesApi.listStaff(syndicate.id, { signal })
      const member = findMember(staff, userId)
      if (member) memberships.push({ syndicateId: syndicate.id, member })
    } catch (error) {
      // Not on this syndicate's staff
      if (isApiError(error) && error.status === 403) continue
      throw error
    }
  }
  return memberships
}
