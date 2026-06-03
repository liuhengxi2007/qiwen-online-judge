export type ContestPenaltyMillis = number & { readonly __brand: 'ContestPenaltyMillis' }

export function contestPenaltyMillisValue(penaltyMillis: ContestPenaltyMillis): number {
  return penaltyMillis
}
