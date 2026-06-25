/**
 * 生成用于错误消息的 subtask/testcase 标签，优先包含用户声明的 label。
 */
export function judgeNodeLabel(kind: 'subtask' | 'testcase', index: number, label: unknown): string {
  return typeof label === 'string' && label.trim() ? `${kind} ${index} (${label.trim()})` : `${kind} ${index}`
}
