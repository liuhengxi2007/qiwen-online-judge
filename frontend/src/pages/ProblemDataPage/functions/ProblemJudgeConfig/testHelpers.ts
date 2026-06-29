import { parseProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNode } from '@/objects/problem/response/ProblemDataTreeNode'

export function file(path: string): ProblemDataTreeNode {
  const parsed = parseProblemDataPath(path)
  if (!parsed.ok) {
    throw new Error(parsed.error)
  }
  return { path: parsed.value, kind: 'file', sizeBytes: 1 }
}

export const templateFiles = [
  file('judge.yaml'),
  file('validators/validator.cpp'),
  file('solutions/std.cpp'),
  file('tools/interactor.cpp'),
  file('tools/strategy.cpp'),
  file('stubs/main.cpp'),
  file('headers/xxx.h'),
  file('headers/other/xxx.h'),
  file('headers/readme.txt'),
  file('sample/1.in'),
  file('sample/1.ans'),
  file('tests/1.in'),
  file('tests/1.ans'),
]
