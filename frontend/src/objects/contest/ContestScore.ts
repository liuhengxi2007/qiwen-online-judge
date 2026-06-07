export type ContestScore = number & { readonly __brand: 'ContestScore' }

export function contestScoreValue(score: ContestScore): number {
  return score
}

export function formatContestScore(score: ContestScore): string {
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 2,
  }).format(contestScoreValue(score) * 100)
}
