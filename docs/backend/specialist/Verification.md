# Specialist verification

A specialist is hidden from public search until a reviewer verifies them. Reviewers are:

- **Admin**: can review every specialist and credential.
- **Syndicate staff**: can review only specialists of their own syndicate.

All requests below need a logged-in reviewer (cookie session).

## The flow

1. The specialist signs up, onboards and chooses their syndicate, specialty and member number. Their status is `pending`.
2. The reviewer verifies or rejects the specialist.
3. When the status is `active`, the profile shows in search.
4. The specialist uploads credentials (degree, license, and so on). Each credential is `pending`.
5. A reviewer opens the credential queue and checks each document.
6. The reviewer approves or rejects each credential.

## Two separate decisions

|            | Credential                                                   | Specialist                                  |
|------------|--------------------------------------------------------------|---------------------------------------------|
| Statuses   | `pending`, `verified`, `rejected`                            | `pending`, `active`, `rejected`             |
| Decided by | Reviewer, one by one                                         | Reviewer, once per specialist               |
| Effect     | A verified, unexpired credential shows on the public profile | `active` makes the profile appear in search |

They do not affect each other. Verifying every credential does **not** make the specialist `active`, and verifying the specialist does not check their credentials. The reviewer decides both.

## Review credentials

| Action                          | Request                                                                                        |
|---------------------------------|------------------------------------------------------------------------------------------------|
| Credential queue of a syndicate | `GET /syndicates/{syndicateId}/credentials?verification_status=pending`                        |
| Open the document               | `GET /credentials/{credentialId}/document` (temporary link)                                    |
| Approve                         | `POST /credentials/{credentialId}/verify`                                                      |
| Reject                          | `POST /credentials/{credentialId}/reject` with `{ "rejection_reason": "Document unreadable" }` |

Each queue item has a `specialist_id`, which you can use for the specialist actions below.

## Review the specialist

| Action                | Request                                                                              | Result                                                                         |
|-----------------------|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| View (even if hidden) | `GET /specialists/by-id/{specialistId}`                                              | Pending, rejected and suspended profiles included                              |
| Verify                | `POST /specialists/by-id/{specialistId}/verify`                                      | Status `active`, appears in search. Safe to repeat                             |
| Reject                | `POST /specialists/by-id/{specialistId}/reject` with `{ "rejection_reason": "..." }` | Status `rejected`, leaves search. A later verify brings them back              |
| Suspend               | `POST /specialists/by-id/{specialistId}/suspend`                                     | Account suspended: cannot log in, leaves search. Verification status unchanged |
| Lift suspension       | `POST /specialists/by-id/{specialistId}/unsuspend`                                   | Account active again. A pending account stays pending                          |

Rules:

- `rejection_reason` is required and up to 1000 characters. Missing or too long returns `422`.
- Staff of another syndicate get `403`. An unknown id returns `404`.
- Suspend and unsuspend return `409` if the account is deleted.
- Verify and reject do not affect login. Only suspension does.
- After a specialist is `active`, they can no longer change their syndicate (`409 SYNDICATE_LOCKED`).

## When does a specialist show in search?

All of these must be true:

- verification status is `active`
- the account is `active` (not suspended or pending)
- the profile and the account are not deleted

## What the specialist sees

Their dashboard (`GET /specialists/me/dashboard`) shows `verification_status` and `pending_credentials_count`. A rejection reason is returned on their own profile (`GET /specialists/me`) so the app can tell them why.