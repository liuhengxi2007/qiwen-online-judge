import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { UpdateContestRequest } from '@/objects/contest/request/UpdateContestRequest'
import { contestDescriptionValue, parseContestDescription } from '@/objects/contest/ContestDescription'
import { contestTitleValue, parseContestTitle } from '@/objects/contest/ContestTitle'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import {
  buildResourceAccessPolicy,
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/pages/components/ResourceAccessEditorInput'

/**
 * 比赛管理表单草稿，保存标题、描述、时间和题目关联输入。
 */
export type ContestManageDraft = {
  title: string
  description: string
  startAt: string
  endAt: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  grantedManagerUsersInput: string
  grantedManagerGroupsInput: string
}

/**
 * 比赛管理草稿校验结果，成功时携带更新请求，失败时携带用户可见错误。
 */
type ValidateContestManageDraftResult =
  | { ok: true; request: UpdateContestRequest }
  | { ok: false; message: string }

/**
 * 从比赛详情构造管理表单草稿，把 ISO 时间转换为本地 datetime-local 输入格式。
 */
export function contestManageDraftFromDetail(contest: ContestDetail): ContestManageDraft {
  return {
    title: contestTitleValue(contest.title),
    description: contestDescriptionValue(contest.description),
    startAt: toLocalDateTimeInput(contest.startAt),
    endAt: toLocalDateTimeInput(contest.endAt),
    baseAccess: contest.accessPolicy.baseAccess,
    grantedUsersInput: grantedUsersInputFromAccessPolicy(contest.accessPolicy),
    grantedGroupsInput: grantedGroupsInputFromAccessPolicy(contest.accessPolicy),
    grantedManagerUsersInput: grantedManagerUsersInputFromAccessPolicy(contest.accessPolicy),
    grantedManagerGroupsInput: grantedManagerGroupsInputFromAccessPolicy(contest.accessPolicy),
  }
}

/**
 * 校验比赛管理草稿，解析标题、描述和开始/结束时间后生成更新请求。
 */
export function validateContestManageDraft(draft: ContestManageDraft): ValidateContestManageDraftResult {
  const title = parseContestTitle(draft.title)
  if (!title.ok) {
    return { ok: false, message: title.error }
  }

  const description = parseContestDescription(draft.description)
  if (!description.ok) {
    return { ok: false, message: description.error }
  }

  const startAt = parseLocalDateTime(draft.startAt, 'Contest start time is required.')
  if (!startAt.ok) {
    return startAt
  }

  const endAt = parseLocalDateTime(draft.endAt, 'Contest end time is required.')
  if (!endAt.ok) {
    return endAt
  }

  if (new Date(endAt.value).getTime() <= new Date(startAt.value).getTime()) {
    return { ok: false, message: 'Contest end time must be after start time.' }
  }

  const accessPolicy = buildResourceAccessPolicy(
    draft.baseAccess,
    draft.grantedUsersInput,
    draft.grantedGroupsInput,
    draft.grantedManagerUsersInput,
    draft.grantedManagerGroupsInput,
  )
  if (!accessPolicy.ok) {
    return { ok: false, message: accessPolicy.message }
  }

  return {
    ok: true,
    request: {
      title: title.value,
      description: description.value,
      startAt: startAt.value,
      endAt: endAt.value,
      accessPolicy: accessPolicy.value,
    },
  }
}

/**
 * 将 datetime-local 输入解析为 ISO 字符串；空值或非法日期返回指定错误。
 */
function parseLocalDateTime(rawValue: string, emptyMessage: string): { ok: true; value: string } | { ok: false; message: string } {
  if (!rawValue.trim()) {
    return { ok: false, message: emptyMessage }
  }

  const date = new Date(rawValue)
  if (Number.isNaN(date.getTime())) {
    return { ok: false, message: 'Contest time must be a valid date and time.' }
  }

  return { ok: true, value: date.toISOString() }
}

/**
 * 将 ISO 时间转换为 datetime-local 控件需要的本地时间字符串。
 */
function toLocalDateTimeInput(isoValue: string): string {
  const date = new Date(isoValue)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const timezoneOffsetMillis = date.getTimezoneOffset() * 60 * 1000
  return new Date(date.getTime() - timezoneOffsetMillis).toISOString().slice(0, 16)
}
