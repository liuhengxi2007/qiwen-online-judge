import type { ToolLimitsConfig } from './JudgeConfigLimits'

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
 * 标准答案生成配置，区分未声明、显式禁用和生成器文件。
 */
export type StandardConfig =
  | { type: 'unspecified' }
  | { type: 'none' }
  | { type: 'generator'; path: string }
