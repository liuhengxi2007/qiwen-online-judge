export type HackSubtaskAvailability = {
  subtaskIndex: number
  canHack: boolean
  reason: 'hack_disabled' | 'score_already_zero' | null
}
