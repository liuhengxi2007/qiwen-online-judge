/**
 * 生成子任务或测试点标题，保留后端返回的可选 label 以辅助定位。
 */
export function resultNodeTitle(kind: 'subtask' | 'testcase', index: number, label: string | null): string {
  return label ? `${kind} ${index} (${label})` : `${kind} ${index}`
}
