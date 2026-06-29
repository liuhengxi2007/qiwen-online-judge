import { describe, expect, it } from 'vitest'

import { ClearProblemData } from '@/apis/problem/ClearProblemData'
import { DeleteProblem } from '@/apis/problem/DeleteProblem'
import { DeleteProblemDataPath } from '@/apis/problem/DeleteProblemDataPath'
import { DownloadProblemDataArchive } from '@/apis/problem/DownloadProblemDataArchive'
import { DownloadProblemDataPath } from '@/apis/problem/DownloadProblemDataPath'
import { ListManageableProblemSuggestions } from '@/apis/problem/ListManageableProblemSuggestions'
import { ListProblemDataFiles } from '@/apis/problem/ListProblemDataFiles'
import { ListProblemDataTree } from '@/apis/problem/ListProblemDataTree'
import { SetProblemDataReady } from '@/apis/problem/SetProblemDataReady'
import { UpdateProblem } from '@/apis/problem/UpdateProblem'
import { UploadProblemDataArchive } from '@/apis/problem/UploadProblemDataArchive'
import { UploadProblemDataFile } from '@/apis/problem/UploadProblemDataFile'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { UpdateProblemRequest } from '@/objects/problem/request/UpdateProblemRequest'

const contestSlug = 'sample-contest' as ContestSlug
const problemSlug = 'two-sum' as ProblemSlug
const dataPath = 'cases/main.in' as ProblemDataPath
const filename = 'main.in' as ProblemDataFilename
const query = 'two' as ProblemSearchQuery
const updateRequest = {} as UpdateProblemRequest

function file(): File {
  return new File(['content'], 'data.zip')
}

describe('problem API paths', () => {
  it('uses problem-owned mutation paths with optional contest context', () => {
    expect(new UpdateProblem(problemSlug, updateRequest, contestSlug).apiPath).toBe('problems/two-sum?contestSlug=sample-contest')
    expect(new DeleteProblem(problemSlug, contestSlug).apiPath).toBe('problems/two-sum/delete?contestSlug=sample-contest')
  })

  it('uses problem-owned data paths with optional contest context', () => {
    const apiPaths = [
      new ListProblemDataFiles(problemSlug, contestSlug).apiPath,
      new ListProblemDataTree(problemSlug, contestSlug).apiPath,
      new DeleteProblemDataPath(problemSlug, dataPath, contestSlug).apiPath,
      new ClearProblemData(problemSlug, contestSlug).apiPath,
      new SetProblemDataReady(problemSlug, true, contestSlug).apiPath,
      new UploadProblemDataFile(problemSlug, file(), filename, contestSlug).apiPath,
      new UploadProblemDataArchive(problemSlug, file(), contestSlug).apiPath,
      new DownloadProblemDataArchive(problemSlug, contestSlug).apiPath,
    ]

    expect(apiPaths).toEqual([
      'problems/two-sum/data/files?contestSlug=sample-contest',
      'problems/two-sum/data/files/tree?contestSlug=sample-contest',
      'problems/two-sum/data/files/delete?contestSlug=sample-contest',
      'problems/two-sum/data/files/delete-all?contestSlug=sample-contest',
      'problems/two-sum/data/ready-state?contestSlug=sample-contest',
      'problems/two-sum/data/files?contestSlug=sample-contest',
      'problems/two-sum/data/archive-imports?contestSlug=sample-contest',
      'problems/two-sum/data/archive-downloads?contestSlug=sample-contest',
    ])
    expect(apiPaths.every((path) => !path.startsWith('contests/'))).toBe(true)
  })

  it('preserves download path query while appending contest context', () => {
    expect(new DownloadProblemDataPath(problemSlug, dataPath, contestSlug).apiPath).toBe(
      'problems/two-sum/data/files/download?path=cases%2Fmain.in&contestSlug=sample-contest',
    )
    expect(new DownloadProblemDataArchive(problemSlug, contestSlug).apiPath).toBe(
      'problems/two-sum/data/archive-downloads?contestSlug=sample-contest',
    )
  })

  it('uses problem-owned manageable suggestions path with optional contest context', () => {
    expect(new ListManageableProblemSuggestions(query, contestSlug).apiPath).toBe(
      'problem-suggestions/manageable?q=two&contestSlug=sample-contest',
    )
  })
})
