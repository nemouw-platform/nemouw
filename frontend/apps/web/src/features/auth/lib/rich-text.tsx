/**
 * Tags for `t.rich(...)` in auth messages, e.g.
 * "We sent a code to <strong>{email}</strong>".
 */
export const richText = {
  // <bdi> keeps Latin text such as an email in its own direction inside
  // Arabic sentences; without it the bidi algorithm scrambles it
  strong: (chunks: React.ReactNode) => (
    <strong className="font-medium text-foreground">
      <bdi>{chunks}</bdi>
    </strong>
  ),
}
