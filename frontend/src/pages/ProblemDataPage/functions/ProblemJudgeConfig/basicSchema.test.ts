import { describe, expect, it } from 'vitest'

import {
  judgeConfigTemplate,
  validateJudgeConfigYaml,
} from '../ProblemJudgeConfig'
import { templateFiles } from './testHelpers'

describe('problem-judge-config basic schema', () => {
  it('accepts the default template', () => {
    expect(validateJudgeConfigYaml(judgeConfigTemplate, templateFiles).ok).toBe(true)
  })

  it('rejects missing version', () => {
    const result = validateJudgeConfigYaml(judgeConfigTemplate.replace('version: 2\n', ''), templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('version must be 2.')
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
    const yaml = `version: 2
hack: false
aggregation:
  testcases: sum_max_max
subtasks:
  - testcases:
      - input: tests/1.in
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('Limits are required for subtask 1 testcase 1.')
      expect(result.errors).toContain('Checker is required for subtask 1 testcase 1.')
    }
  })

  it('rejects testcase-level validator declarations', () => {
    const yaml = `version: 2
hack: false
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
      - input: tests/1.in
        answer: tests/1.ans
        validator:
          path: validators/validator.cpp
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('subtask 1 testcase 1.validator cannot be declared on a testcase.')
    }
  })

  it('requires input and requires answer for builtin exact checker', () => {
    const yaml = `version: 2
hack: false
limits:
  timeMs: 1000
  memoryMb: 256
validator:
  path: validators/validator.cpp
checker:
  type: builtin
  name: exact
aggregation:
  testcases: sum_max_max
subtasks:
  - testcases:
      - label: "1"
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('subtask 1 testcase 1 (1).input is required.')
      expect(result.errors).toContain('subtask 1 testcase 1 (1).answer is required for builtin exact checker.')
    }
  })

  it('rejects out-of-range limits', () => {
    const yaml = `version: 2
hack: false
limits:
  timeMs: 600001
  memoryMb: 65537
validator:
  path: validators/validator.cpp
checker:
  type: builtin
  name: exact
aggregation:
  testcases: sum_max_max
subtasks:
  - testcases:
      - input: tests/1.in
        answer: tests/1.ans
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('limits.timeMs must be between 1 and 600000.')
      expect(result.errors).toContain('limits.memoryMb must be between 1 and 65536.')
    }
  })
})
