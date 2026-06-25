import type { useSiteManageModel } from '../../hooks/useSiteManageModel'

/**
 * 站点管理模型类型别名，供用户管理卡片及其子组件共享。
 */
export type SiteManageModel = ReturnType<typeof useSiteManageModel>

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
