export type BlogVote = 'up' | 'down'

export function fromBlogVoteContract(value: unknown): BlogVote {
  switch (value) {
    case 'up':
    case 'down':
      return value
    default:
      throw new Error('Invalid blog vote in contract payload.')
  }
}

export function toBlogVoteContract(value: BlogVote): BlogVote {
  return value
}
