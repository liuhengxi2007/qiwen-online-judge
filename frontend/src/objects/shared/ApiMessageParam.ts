/** API 消息参数的可序列化值；用于错误/成功提示模板的占位参数，边界是纯前端展示数据。 */
export type ApiMessageParam =
  | { kind: 'text'; value: string }
  | { kind: 'int'; value: number }
  | { kind: 'long'; value: number }
  | { kind: 'bool'; value: boolean }
