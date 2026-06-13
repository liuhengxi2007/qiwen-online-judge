import type { HackSubtaskAvailability } from '@/objects/hack/response/HackSubtaskAvailability'

/** 某个提交的 Hack 可用性响应；按子任务列出可攻击状态。 */
export type SubmissionHackAvailability = {
  subtasks: HackSubtaskAvailability[]
}
