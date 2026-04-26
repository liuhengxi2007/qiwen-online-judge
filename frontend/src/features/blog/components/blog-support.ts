export function formatBlogDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function blogScoreClassName(score: number): string {
  if (score > 0) {
    return 'text-emerald-700'
  }

  if (score < 0) {
    return 'text-rose-700'
  }

  return 'text-slate-700'
}
