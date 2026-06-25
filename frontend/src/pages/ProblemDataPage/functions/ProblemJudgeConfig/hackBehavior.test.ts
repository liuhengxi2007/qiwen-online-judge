import { describe, expect, it } from 'vitest'

import { validateJudgeConfigYaml } from '../ProblemJudgeConfig'
import { file, templateFiles } from './testHelpers'

describe('problem-judge-config hack behavior', () => {
  it('defaults hack to enabled and accepts standard false', () => {
    const yaml = `version: 2
validator:
  path: validators/validator.cpp
standard: false
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
`

    expect(validateJudgeConfigYaml(yaml, templateFiles).ok).toBe(true)
  })

  it('rejects enabled hack without validator or standard', () => {
    const missingValidator = validateJudgeConfigYaml(
      `version: 2
standard: false
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
`,
      templateFiles,
    )

    expect(missingValidator.ok).toBe(false)
    if (!missingValidator.ok) {
      expect(missingValidator.errors).toContain('Validator is required for subtask 1 when hack is enabled.')
    }

    const missingStandard = validateJudgeConfigYaml(
      `version: 2
validator:
  path: validators/validator.cpp
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
`,
      templateFiles,
    )

    expect(missingStandard.ok).toBe(false)
    if (!missingStandard.ok) {
      expect(missingStandard.errors).toContain('standard must be declared as an answer generator object or false for subtask 1 when hack is enabled.')
    }
  })

  it('accepts enabled hack with an answer generator and subtask overrides', () => {
    const objectStandard = `version: 2
hack: true
validator:
  path: validators/validator.cpp
standard:
  language: cpp17
  path: solutions/std.cpp
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
`

    expect(validateJudgeConfigYaml(objectStandard, templateFiles).ok).toBe(true)

    const overrides = `version: 2
hack: false
validator:
  path: validators/validator.cpp
standard: false
limits:
  timeMs: 1000
  memoryMb: 256
checker:
  type: builtin
  name: exact
aggregation:
  testcases: sum_max_max
subtasks:
  - label: disabled
    testcases:
      - input: tests/1.in
        answer: tests/1.ans
  - label: enabled
    hack: true
    standard:
      language: cpp17
      path: solutions/std.cpp
    testcases:
      - input: tests/1.in
        answer: tests/1.ans
`

    expect(validateJudgeConfigYaml(overrides, templateFiles).ok).toBe(true)
  })

  it('rejects testcase-level hack declarations', () => {
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
        hack: true
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('subtask 1 testcase 1.hack cannot be declared on a testcase.')
    }
  })

  it('accepts answerless hack testcase with builtin exact checker', () => {
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
      - type: hack
        input: hacks/7.in
      - input: tests/1.in
        answer: tests/1.ans
`

    expect(validateJudgeConfigYaml(yaml, [...templateFiles, file('hacks/7.in')]).ok).toBe(true)
  })
})
