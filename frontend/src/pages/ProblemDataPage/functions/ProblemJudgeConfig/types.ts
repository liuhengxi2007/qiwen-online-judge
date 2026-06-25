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

export type IntegerRange = {
  min: number
  max: number
}

export type IntegerRangeValidationInput = {
  value: unknown
  label: string
  range: IntegerRange
  ctx: ValidationContext
}

/**
 * 普通评测资源限制配置，单位与 judge.yaml 字段保持一致。
 */
export type LimitsConfig = {
  timeMs: number
  memoryMb: number
}

/**
 * 工具程序资源限制配置，用于 interactor、strategyProvider 等受限工具。
 */
export type ToolLimitsConfig = {
  timeMs: number
  memoryMb: number
}

/**
 * 工具程序配置，包含必需路径和可选资源限制。
 */
export type ToolConfig = {
  path: string
  limits?: ToolLimitsConfig
}

/**
 * checker 配置，支持内置 exact/echo 和 C++17 自定义 checker。
 */
export type CheckerConfig =
  | { type: 'builtin'; name: 'exact' }
  | { type: 'builtin'; name: 'echo' }
  | { type: 'cpp17'; path: string }

/**
 * 判题模式配置，区分传统单角色和交互式多角色。
 */
export type ModeConfig =
  | { type: 'traditional'; role: string }
  | { type: 'interactive'; roles: string[]; interactor: ToolConfig }

/**
 * 聚合策略配置，允许根级和 subtask 级分别声明 testcase/subtask 聚合方式。
 */
export type AggregationConfig = {
  testcases?: string
  subtasks?: string
}

/**
 * 标准答案生成配置，区分未声明、显式禁用和生成器文件。
 */
export type StandardConfig =
  | { type: 'unspecified' }
  | { type: 'none' }
  | { type: 'generator'; path: string }

/**
 * 带原始下标的 YAML 节点值，用于错误消息指向列表位置。
 */
export type IndexedValue = {
  value: unknown
  index: number
}

/**
 * 十进制数字的整数化表示，用于无浮点误差地累加 scoreRatio。
 */
export type DecimalUnits = {
  units: bigint
  scale: number
}
