export type ContestProblemAlias = string & { readonly __brand: 'ContestProblemAlias' }

export function contestProblemAliasValue(alias: ContestProblemAlias): string {
  return alias
}
