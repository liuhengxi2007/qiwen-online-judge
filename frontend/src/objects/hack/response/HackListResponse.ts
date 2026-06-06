import type { PageResponse } from '@/objects/shared/PageResponse'
import type { HackSummary } from '@/objects/hack/response/HackSummary'

export type HackListResponse = PageResponse<HackSummary>
