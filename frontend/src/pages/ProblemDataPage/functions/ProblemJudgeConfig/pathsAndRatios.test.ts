import { describe, expect, it } from 'vitest'

import {
  judgeConfigTemplate,
  validateJudgeConfigYaml,
} from '../ProblemJudgeConfig'
import { templateFiles } from './testHelpers'

describe('problem-judge-config paths and ratios', () => {
  it('rejects invalid file references and bad score ratios', () => {
    const yaml = judgeConfigTemplate.replace('answer: tests/1.ans', 'answer: tests/missing.ans')

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('subtask 1 (main) testcase 2 (1).answer does not exist: tests/missing.ans.')
    }

    const invalidRatio = validateJudgeConfigYaml(
      judgeConfigTemplate.replace('  - label: main\n', '  - label: main\n    scoreRatio: 1.1\n'),
      templateFiles,
    )
    expect(invalidRatio.ok).toBe(false)
    if (!invalidRatio.ok) {
      expect(invalidRatio.errors).toContain('subtasks[0].scoreRatio must be between 0 and 1.')
    }

    const invalidSum = validateJudgeConfigYaml(
      `version: 2
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
  - scoreRatio: 0.7
    testcases:
      - input: tests/1.in
        answer: tests/1.ans
  - scoreRatio: 0.4
    testcases:
      - input: tests/1.in
        answer: tests/1.ans
`,
      templateFiles,
    )
    expect(invalidSum.ok).toBe(false)
    if (!invalidSum.ok) {
      expect(invalidSum.errors).toContain('subtasks explicit scoreRatio values must not sum above 1.')
    }
  })

  it('accepts decimal score ratios that sum exactly to one', () => {
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
  - scoreRatio: 0.33
    testcases:
      - input: tests/1.in
        answer: tests/1.ans
  - scoreRatio: 0.56
    testcases:
      - input: tests/1.in
        answer: tests/1.ans
  - scoreRatio: 0.11
    testcases:
      - input: tests/1.in
        answer: tests/1.ans
`

    expect(validateJudgeConfigYaml(yaml, templateFiles).ok).toBe(true)
  })

  it('rejects scoreRatio on sample and hack testcases', () => {
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
      - type: sample
        scoreRatio: 0
        input: sample/1.in
        answer: sample/1.ans
      - type: hack
        scoreRatio: 0
        input: sample/1.in
        answer: sample/1.ans
      - input: tests/1.in
        answer: tests/1.ans
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('subtask 1 testcase 1.scoreRatio cannot be declared when type is sample.')
      expect(result.errors).toContain('subtask 1 testcase 2.scoreRatio cannot be declared when type is hack.')
    }
  })

  it('rejects subtasks without a main testcase', () => {
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
  - label: sample
    testcases:
      - type: sample
        input: sample/1.in
        answer: sample/1.ans
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('subtask 1 (sample) must define at least one main testcase.')
    }
  })
})
