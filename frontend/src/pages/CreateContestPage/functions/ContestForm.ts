import type { CreateContestRequest } from '@/objects/contest/request/CreateContestRequest'
import { parseContestDescription } from '@/objects/contest/ContestDescription'
import { parseContestSlug } from '@/objects/contest/ContestSlug'
import { parseContestTitle } from '@/objects/contest/ContestTitle'
import { buildResourceAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'

/**
 * 创建比赛表单草稿，保存标题、描述和时间输入。
 */
export type CreateContestDraft = {
  slug: string
  title: string
  description: string
  startAt: string
  endAt: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

/**
 * 创建比赛草稿校验结果，成功时携带创建请求，失败时携带错误消息。
 */
type ValidateContestDraftResult =
  | { ok: true; request: CreateContestRequest }
  | { ok: false; message: string }

/**
 * 校验创建比赛表单，解析标题、描述和开始/结束时间。
 */
export function validateContestDraft(draft: CreateContestDraft): ValidateContestDraftResult {
  const slug = parseContestSlug(draft.slug)
  if (!slug.ok) {
    return { ok: false, message: slug.error }
  }

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

  const accessPolicy = buildResourceAccessPolicy({
    baseAccess: draft.baseAccess,
    viewer: {
      usersInput: draft.grantedUsersInput,
      groupsInput: draft.grantedGroupsInput,
    },
  })
  if (!accessPolicy.ok) {
    return { ok: false, message: accessPolicy.message }
  }

  return {
    ok: true,
    request: {
      slug: slug.value,
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
