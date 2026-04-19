export function contributionTextClassName(value: number): string {
  if (value > 0) {
    return 'text-emerald-700'
  }

  if (value < 0) {
    return 'text-rose-700'
  }

  return 'text-slate-700'
}
