import type { useSiteManageModel } from '../../../hooks/useSiteManageModel'

/**
 * 站点管理模型类型别名，供用户管理卡片及其子组件共享。
 */
export type SiteManageModel = ReturnType<typeof useSiteManageModel>
