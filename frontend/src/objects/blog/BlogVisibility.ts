export type BlogVisibility = 'public' | 'private'

export function fromBlogVisibilityContract(value: unknown): BlogVisibility {
  switch (value) {
    case 'public':
    case 'private':
      return value
    default:
      throw new Error('Invalid blog visibility in contract payload.')
  }
}

export function toBlogVisibilityContract(value: BlogVisibility): BlogVisibility {
  return value
}
