export type ContestRank = number & { readonly __brand: 'ContestRank' }

export function contestRankValue(rank: ContestRank): number {
  return rank
}
