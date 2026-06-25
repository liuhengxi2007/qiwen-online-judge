import type { ToolConfig } from './JudgeConfigTool'

/**
 * 判题模式配置，区分传统单角色和交互式多角色。
 */
export type ModeConfig =
  | { type: 'traditional'; role: string }
  | { type: 'interactive'; roles: string[]; interactor: ToolConfig }
