/**
 * 从 URL 查询参数归一化榜单页码，非法、缺失或非正数都回退第一页。
 */
export function normalizePage(rawPage: string | null): number {
  const parsedPage = rawPage ? Number.parseInt(rawPage, 10) : 1
  return Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 1
}

/**
 * 构造榜单页路径，同时保留贡献、AC 数和 rating 三个榜单各自的页码。
 */
export function pagePath(contributionPage: number, acceptedPage: number, ratingPage: number): string {
  return `/ranklist?contributionPage=${contributionPage}&acceptedPage=${acceptedPage}&ratingPage=${ratingPage}`
}
