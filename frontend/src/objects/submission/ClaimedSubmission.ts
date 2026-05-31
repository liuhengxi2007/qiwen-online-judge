import type { ProblemId } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'

type ClaimedSubmissionProgram = {
  language: SubmissionLanguage
  sourceKey: string
  sizeBytes: number
  sha256: string
}

type ClaimedSubmissionProgramManifest = {
  defaultProgramKey: string
  programs: Record<string, ClaimedSubmissionProgram>
}

export type ClaimedSubmission = {
  id: SubmissionId
  problemId: ProblemId
  problemSlug: ProblemSlug
  programManifest: ClaimedSubmissionProgramManifest
}
