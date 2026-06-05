import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { ContestId } from '@/objects/contest/ContestId'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemId } from '@/objects/problem/ProblemId'
import { useContestManagePageModel } from './useContestManagePageModel'

const apiClient = vi.hoisted(() => ({
  sendAPI: vi.fn(),
}))

const i18nClient = vi.hoisted(() => ({
  t: (key: string) => key,
}))

vi.mock('@/system/api/api-message', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/system/api/api-message')>()
  return {
    ...actual,
    sendAPI: apiClient.sendAPI,
  }
})

vi.mock('@/system/i18n/use-i18n', () => ({
  useI18n: () => i18nClient,
}))

const contestSlug = 'sample-contest' as ContestSlug
const problemSlug = 'two-sum' as ProblemSlug

function contestDetail(overrides: Partial<ContestDetail> = {}): ContestDetail {
  return {
    id: '00000000-0000-0000-0000-000000000001' as ContestId,
    slug: contestSlug,
    title: 'Sample Contest' as ContestDetail['title'],
    description: '' as ContestDetail['description'],
    startAt: '2026-06-05T10:00',
    endAt: '2026-06-05T12:00',
    problems: [],
    accessPolicy: {
      baseAccess: 'restricted',
      viewerGrants: [],
      managerGrants: [],
    },
    registrationStatus: {
      isRegistered: false,
    },
    canManage: true,
    author: null,
    createdAt: '2026-06-05T00:00:00Z',
    updatedAt: '2026-06-05T00:00:00Z',
    ...overrides,
  }
}

function contestAfterAttach(): ContestDetail {
  return contestDetail({
    problems: [
      {
        id: '00000000-0000-0000-0000-000000000002' as ProblemId,
        slug: problemSlug,
        title: 'Two Sum' as ContestDetail['problems'][number]['title'],
        position: 1,
        alias: 'A' as ContestDetail['problems'][number]['alias'],
      },
    ],
  })
}

function installApiMock(shouldWarn: boolean) {
  apiClient.sendAPI.mockImplementation((message: { method: string; apiPath: string }) => {
    if (message.method === 'GET' && message.apiPath === 'contests/sample-contest') {
      return Promise.resolve(contestDetail())
    }

    if (message.method === 'GET' && message.apiPath === 'problem-suggestions/manageable?q=two&contestSlug=sample-contest') {
      return Promise.resolve([{ slug: problemSlug, title: 'Two Sum' }])
    }

    if (message.method === 'GET' && message.apiPath === 'contests/sample-contest/problems/two-sum/attach-warning') {
      return Promise.resolve({ shouldWarn })
    }

    if (message.method === 'POST' && message.apiPath === 'contests/sample-contest/problems') {
      return Promise.resolve(contestAfterAttach())
    }

    return Promise.resolve([])
  })
}

async function loadedModel() {
  const rendered = renderHook(() => useContestManagePageModel(contestSlug), {
    reactStrictMode: false,
  })

  await waitFor(() => {
    expect(rendered.result.current.isLoading).toBe(false)
  })

  act(() => {
    rendered.result.current.setProblemSearchInput('two-sum')
  })

  return rendered
}

describe('useContestManagePageModel problem attach warning', () => {
  beforeEach(() => {
    apiClient.sendAPI.mockReset()
  })

  it('attaches immediately when the preflight does not warn', async () => {
    installApiMock(false)
    const rendered = await loadedModel()

    await act(async () => {
      await rendered.result.current.attachProblem()
    })

    expect(apiClient.sendAPI).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'GET',
        apiPath: 'contests/sample-contest/problems/two-sum/attach-warning',
      }),
    )
    expect(apiClient.sendAPI).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'POST',
        apiPath: 'contests/sample-contest/problems',
      }),
    )
    expect(rendered.result.current.isAttachWarningOpen).toBe(false)
  })

  it('loads attach suggestions through the problem-owned manageable suggestions API', async () => {
    installApiMock(false)
    const rendered = await loadedModel()

    act(() => {
      rendered.result.current.setIsProblemSearchFocused(true)
      rendered.result.current.setProblemSearchInput('two')
    })

    await waitFor(() => {
      expect(apiClient.sendAPI).toHaveBeenCalledWith(
        expect.objectContaining({
          method: 'GET',
          apiPath: 'problem-suggestions/manageable?q=two&contestSlug=sample-contest',
        }),
      )
    })
    expect(rendered.result.current.problemSuggestions).toEqual([{ slug: problemSlug, title: 'Two Sum' }])
  })

  it('opens confirmation and skips attach when cancelled', async () => {
    installApiMock(true)
    const rendered = await loadedModel()

    await act(async () => {
      await rendered.result.current.attachProblem()
    })

    expect(rendered.result.current.isAttachWarningOpen).toBe(true)
    expect(apiClient.sendAPI).not.toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'POST',
        apiPath: 'contests/sample-contest/problems',
      }),
    )

    act(() => {
      rendered.result.current.closeAttachProblemWarning(false)
    })

    expect(rendered.result.current.isAttachWarningOpen).toBe(false)
    expect(apiClient.sendAPI).not.toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'POST',
        apiPath: 'contests/sample-contest/problems',
      }),
    )
  })

  it('attaches after confirming the warning', async () => {
    installApiMock(true)
    const rendered = await loadedModel()

    await act(async () => {
      await rendered.result.current.attachProblem()
    })

    await act(async () => {
      await rendered.result.current.confirmAttachProblemWarning()
    })

    expect(apiClient.sendAPI).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'POST',
        apiPath: 'contests/sample-contest/problems',
      }),
    )
    expect(rendered.result.current.isAttachWarningOpen).toBe(false)
  })
})
