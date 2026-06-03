export type ContestScore = number & { readonly __brand: 'ContestScore' }

export function contestScoreValue(score: ContestScore): number {
  return score
}
