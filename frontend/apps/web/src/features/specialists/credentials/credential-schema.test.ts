import type { CredentialResponse } from "@workspace/api"
import { describe, expect, it } from "vitest"

import {
  createCredentialSchema,
  toCredentialFields,
  toCredentialValues,
  type CredentialValues,
} from "@/features/specialists/credentials/credential-schema"
import type { FormErrorsTranslator } from "@/lib/forms/api-errors"

const t = ((key: string, values?: object) =>
  values ? `${key} ${JSON.stringify(values)}` : key) as FormErrorsTranslator
const labels = {
  type: "Type",
  title: "Title",
  organization: "Issued by",
  document: "Document",
}
const schemaFor = (requireDocument: boolean) =>
  createCredentialSchema(
    labels,
    t,
    { dateOrder: "dateOrder" },
    { requireDocument }
  )

const filled: CredentialValues = {
  credential_type: "license",
  title: "Licence to practise",
  issuing_organization: "Order of Speech Therapists",
  credential_number: "",
  issue_date: "2020-05-03",
  expiry_date: "",
  document: new File(["%PDF"], "licence.pdf", { type: "application/pdf" }),
}

const issues = (
  requireDocument: boolean,
  values: Partial<CredentialValues>
) => {
  const result = schemaFor(requireDocument).safeParse({ ...filled, ...values })
  return result.success
    ? {}
    : Object.fromEntries(
        result.error.issues.map((issue) => [issue.path[0], issue.message])
      )
}

describe("createCredentialSchema", () => {
  it("accepts a complete credential", () => {
    expect(issues(true, {})).toEqual({})
  })

  it("needs a document for a new credential only", () => {
    expect(issues(true, { document: null })).toEqual({
      document: 'required {"field":"Document"}',
    })
    expect(issues(false, { document: null })).toEqual({})
  })

  it("needs the type, title and issuer", () => {
    expect(
      Object.keys(
        issues(true, {
          credential_type: "",
          title: " ",
          issuing_organization: "",
        })
      )
    ).toEqual(["credential_type", "title", "issuing_organization"])
  })

  it("refuses an expiry before the issue date", () => {
    expect(issues(true, { expiry_date: "2019-01-01" })).toEqual({
      expiry_date: "dateOrder",
    })
    expect(issues(true, { expiry_date: "2020-05-03" })).toEqual({})
  })
})

describe("toCredentialValues / toCredentialFields", () => {
  it("leaves empty optional fields out of the request", () => {
    expect(toCredentialFields(filled)).toEqual({
      credential_type: "license",
      title: "Licence to practise",
      issuing_organization: "Order of Speech Therapists",
      issue_date: "2020-05-03",
    })
  })

  it("fills the form from a saved credential, without a new document", () => {
    const saved: CredentialResponse = {
      credential_type: "degree",
      title: "BSc Speech Therapy",
      issuing_organization: "Lebanese University",
      credential_number: null,
      issue_date: "2018-06-30",
      expiry_date: null,
    }
    expect(toCredentialValues(saved)).toEqual({
      credential_type: "degree",
      title: "BSc Speech Therapy",
      issuing_organization: "Lebanese University",
      credential_number: "",
      issue_date: "2018-06-30",
      expiry_date: "",
      document: null,
    })
  })
})
