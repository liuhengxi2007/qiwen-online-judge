export function normalizePage(rawPage: string | null): number {
  const parsedPage = rawPage ? Number.parseInt(rawPage, 10) : 1
  return Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 1
}

export function pagePath(contributionPage: number, acceptedPage: number): string {
  return `/ranklist?contributionPage=${contributionPage}&acceptedPage=${acceptedPage}`
}
