import { describe, expect, it } from 'vitest'

import {
  judgeConfigTemplate,
  validateJudgeConfigYaml,
} from './ProblemJudgeConfig'
import { parseProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNode } from '@/objects/problem/response/ProblemDataTreeNode'

function file(path: string): ProblemDataTreeNode {
  const parsed = parseProblemDataPath(path)
  if (!parsed.ok) {
    throw new Error(parsed.error)
  }
  return { path: parsed.value, kind: 'file', sizeBytes: 1 }
}

const templateFiles = [
  file('judge.yaml'),
  file('sample/1.in'),
  file('sample/1.ans'),
  file('tests/1.in'),
  file('tests/1.ans'),
]

describe('problem-judge-config', () => {
  it('accepts the default template', () => {
    expect(validateJudgeConfigYaml(judgeConfigTemplate, templateFiles).ok).toBe(true)
  })

  it('rejects missing version', () => {
    const result = validateJudgeConfigYaml(judgeConfigTemplate.replace('version: 1\n', ''), templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('version must be 1.')
    }
  })

  it('rejects old comma aggregation values', () => {
    const result = validateJudgeConfigYaml(judgeConfigTemplate.replaceAll('sum_max_max', 'sum,max,max'), templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors.some((error) => error.includes('aggregation.testcases'))).toBe(true)
    }
  })

  it('rejects missing inherited limits and checker', () => {
    const yaml = `version: 1
aggregation:
  testcases: sum_max_max
subtasks:
  - testcases:
      - answer: tests/1.ans
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('Limits are required for testcase subtasks[0]/testcase-1.')
      expect(result.errors).toContain('Checker is required for testcase subtasks[0]/testcase-1.')
    }
  })

  it('rejects missing answer but allows omitted input', () => {
    const yaml = `version: 1
limits:
  timeMs: 1000
  memoryMb: 256
checker:
  type: builtin
  name: exact
aggregation:
  testcases: sum_max_max
subtasks:
  - testcases:
      - name: "1"
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('subtasks[0]/1.answer is required.')
      expect(result.errors.some((error) => error.includes('.input'))).toBe(false)
    }
  })

  it('rejects out-of-range limits', () => {
    const yaml = `version: 1
limits:
  timeMs: 600001
  memoryMb: 65537
checker:
  type: builtin
  name: exact
aggregation:
  testcases: sum_max_max
subtasks:
  - testcases:
      - answer: tests/1.ans
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('limits.timeMs must be between 1 and 600000.')
      expect(result.errors).toContain('limits.memoryMb must be between 1 and 65536.')
    }
  })

  it('rejects invalid checker declarations and missing checker files', () => {
    const invalidBuiltin = validateJudgeConfigYaml(judgeConfigTemplate.replace('name: exact', 'name: fuzzy'), templateFiles)
    expect(invalidBuiltin.ok).toBe(false)

    const missingCpp = validateJudgeConfigYaml(
      judgeConfigTemplate.replace(
        `type: builtin
  name: exact`,
        `type: cpp
  path: checker/main.cpp`,
      ),
      templateFiles,
    )

    expect(missingCpp.ok).toBe(false)
    if (!missingCpp.ok) {
      expect(missingCpp.errors).toContain('sample/sample-1.checker.path does not exist: checker/main.cpp.')
    }
  })

  it('rejects invalid file references and bad score ratios', () => {
    const yaml = judgeConfigTemplate
      .replace('answer: tests/1.ans', 'answer: tests/missing.ans')
      .replace('scoreRatio: 0.8', 'scoreRatio: 0.9')

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('subtasks explicit scoreRatio values must not sum above 1.')
      expect(result.errors).toContain('main/1.answer does not exist: tests/missing.ans.')
    }

    const invalidRatio = validateJudgeConfigYaml(judgeConfigTemplate.replace('scoreRatio: 0.8', 'scoreRatio: 1.1'), templateFiles)
    expect(invalidRatio.ok).toBe(false)
    if (!invalidRatio.ok) {
      expect(invalidRatio.errors).toContain('subtasks[1].scoreRatio must be between 0 and 1.')
    }
  })
})
