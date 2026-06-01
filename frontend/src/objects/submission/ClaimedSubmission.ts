import type { ProblemId } from '@/objects/problem/ProblemId'
import { fromProblemIdContract } from '@/objects/problem/ProblemId'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { fromProblemSlugContract } from '@/objects/problem/ProblemSlug'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { fromSubmissionIdContract } from '@/objects/submission/SubmissionId'
import type { SubmissionLanguage } from '@/objects/submission/SubmissionLanguage'
import { fromSubmissionLanguageContract } from '@/objects/submission/SubmissionLanguage'
import { readNonNegativeSafeInteger, readRecord, readSafeInteger, readString } from '@/objects/shared/PageResponse'

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

function fromClaimedSubmissionProgramContract(value: unknown, label: string): ClaimedSubmissionProgram {
  const program = readRecord(value, label)
  const sha256 = readString(program.sha256, `${label} sha256`)
  if (!/^[a-f0-9]{64}$/i.test(sha256)) {
    throw new Error(`Invalid ${label} sha256 in contract payload.`)
  }

  return {
    language: fromSubmissionLanguageContract(program.language),
    sourceKey: readString(program.sourceKey, `${label} source key`),
    sizeBytes: readNonNegativeSafeInteger(program.sizeBytes, `${label} size bytes`),
    sha256: sha256.toLowerCase(),
  }
}

function fromClaimedSubmissionProgramManifestContract(
  value: unknown,
  label: string,
): ClaimedSubmissionProgramManifest {
  const manifest = readRecord(value, label)
  const programs = readRecord(manifest.programs, `${label} programs`)
  return {
    defaultProgramKey: readString(manifest.defaultProgramKey, `${label} default program key`),
    programs: Object.fromEntries(
      Object.entries(programs).map(([key, program]) => [
        key,
        fromClaimedSubmissionProgramContract(program, `${label} program ${key}`),
      ]),
    ),
  }
}

export function fromClaimedSubmissionContract(value: unknown, label = 'claimed submission'): ClaimedSubmission {
  const submission = readRecord(value, label)
  return {
    id: fromSubmissionIdContract(readSafeInteger(submission.id, `${label} id`), `${label} id`),
    problemId: fromProblemIdContract(readString(submission.problemId, `${label} problem id`), `${label} problem id`),
    problemSlug: fromProblemSlugContract(readString(submission.problemSlug, `${label} problem slug`), `${label} problem slug`),
    programManifest: fromClaimedSubmissionProgramManifestContract(submission.programManifest, `${label} program manifest`),
  }
}
