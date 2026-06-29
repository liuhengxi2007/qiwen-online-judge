/**
 * 判题配置校验结果；成功仅携带 warning，失败同时返回错误列表和 warning。
 */
export type JudgeConfigValidationResult =
  | { ok: true; warnings: string[] }
  | { ok: false; errors: string[]; warnings: string[] }

/**
 * 判题配置校验上下文，累积错误并保存题目数据中已存在的文件路径集合。
 */
export type ValidationContext = {
  errors: string[]
  filePaths: Set<string>
}

/**
 * 带原始下标的 YAML 节点值，用于错误消息指向列表位置。
 */
export type IndexedValue = {
  value: unknown
  index: number
}
