import { describe, expect, it } from 'vitest'

import { fromRegisteredJudgerListItemContract } from '@/features/judger/domain/judger-contract'

describe('judger-contract', () => {
  it('trims host process id and supported languages from contract payloads', () => {
    const mapped = fromRegisteredJudgerListItemContract({
      judgerId: 'judge-1',
      requestedPrefix: 'judge',
      host: '  worker-host  ',
      processId: ' 1234 ',
      supportedLanguages: [' cpp17 ', '', ' python3 '],
      registeredAt: '2026-04-29T12:00:00Z',
      lastHeartbeatAt: '2026-04-29T12:05:00Z',
    })

    expect(mapped).toEqual({
      judgerId: 'judge-1',
      requestedPrefix: 'judge',
      host: 'worker-host',
      processId: '1234',
      supportedLanguages: ['cpp17', 'python3'],
      registeredAt: '2026-04-29T12:00:00Z',
      lastHeartbeatAt: '2026-04-29T12:05:00Z',
    })
  })

  it('maps missing process ids to null', () => {
    const mapped = fromRegisteredJudgerListItemContract({
      judgerId: 'judge-2',
      requestedPrefix: 'judge',
      host: 'worker-host',
      processId: null,
      supportedLanguages: [],
      registeredAt: '2026-04-29T12:00:00Z',
      lastHeartbeatAt: '2026-04-29T12:05:00Z',
    })

    expect(mapped.processId).toBeNull()
  })
})
