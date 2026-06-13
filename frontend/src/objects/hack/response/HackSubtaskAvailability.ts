/** 单个子任务的 Hack 可用性；reason 为空表示当前可发起 Hack。 */
export type HackSubtaskAvailability = {
  subtaskIndex: number
  canHack: boolean
  reason: 'hack_disabled' | 'score_already_zero' | null
}
