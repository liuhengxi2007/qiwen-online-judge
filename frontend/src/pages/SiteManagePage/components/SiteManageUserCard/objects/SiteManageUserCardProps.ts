import type { SiteManageModel } from './SiteManageModel'

/**
 * 站点管理用户卡片属性，包含模型、查询输入和分页回调。
 */
export type SiteManageUserCardProps = {
  model: SiteManageModel
  siteManagerSession: boolean
  queryInput: string
  hasActiveQuery: boolean
  onQueryInputChange: (value: string) => void
  onApplyQuery: () => void
  onClearQuery: () => void
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}
