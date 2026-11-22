import type { SyndicateStaffResponse } from "@workspace/api"

/** A syndicate the signed-in user is on the staff of, and their place in it */
export interface Membership {
  syndicateId: string
  member: SyndicateStaffResponse
}

/** The user's own row in a syndicate's staff list, if any */
export function findMember(
  staff: SyndicateStaffResponse[],
  userId: string
): SyndicateStaffResponse | undefined {
  return staff.find((member) => member.user?.id === userId)
}
