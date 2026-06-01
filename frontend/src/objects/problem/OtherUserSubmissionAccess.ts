export type OtherUserSubmissionAccess = 'none' | 'summary' | 'detail'

export function fromOtherUserSubmissionAccessContract(
  value: unknown,
): OtherUserSubmissionAccess {
  switch (value) {
    case 'none':
    case 'summary':
    case 'detail':
      return value
    default:
      throw new Error('Invalid other user submission access in contract payload.')
  }
}

export function toOtherUserSubmissionAccessContract(
  value: OtherUserSubmissionAccess,
): OtherUserSubmissionAccess {
  return value
}
