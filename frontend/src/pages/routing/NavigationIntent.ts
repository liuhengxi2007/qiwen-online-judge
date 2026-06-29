/**
 * 页面 hook 返回的导航意图，描述目标路径和是否替换历史记录。
 */
export type NavigationIntent = {
  to: string
  replace?: boolean
}
