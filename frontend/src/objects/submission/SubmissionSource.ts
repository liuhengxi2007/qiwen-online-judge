import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ContestTitle } from '@/objects/contest/ContestTitle'

export type SubmissionSource = {
  contestSlug: ContestSlug | null
  contestTitle: ContestTitle | null
}
