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

type ValidateContestManageDraftResult =
  | { ok: true; request: UpdateContestRequest }
  | { ok: false; message: string }

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

function toLocalDateTimeInput(isoValue: string): string {
  const date = new Date(isoValue)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const timezoneOffsetMillis = date.getTimezoneOffset() * 60 * 1000
  return new Date(date.getTime() - timezoneOffsetMillis).toISOString().slice(0, 16)
}
