import { describe, expect, it } from 'vitest'

import {
  judgeConfigTemplate,
  validateJudgeConfigYaml,
} from '../ProblemJudgeConfig'
import { templateFiles } from './testHelpers'

describe('problem-judge-config tools and checker', () => {
  it('accepts interactive tools with CPU-time limits', () => {
    const yaml = `version: 2
hack: false
mode:
  type: interactive
  roles: [main]
  interactor:
    path: tools/interactor.cpp
    limits:
      timeMs: 1000
      memoryMb: 256
strategyProvider:
  path: tools/strategy.cpp
  limits:
    timeMs: 1000
    memoryMb: 256
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
      - input: tests/1.in
        answer: tests/1.ans
`

    expect(validateJudgeConfigYaml(yaml, templateFiles).ok).toBe(true)
  })

  it('accepts duplicate interactive role instances', () => {
    const yaml = `version: 2
hack: false
mode:
  type: interactive
  roles: [main, main]
  interactor:
    path: tools/interactor.cpp
    limits:
      timeMs: 1000
      memoryMb: 256
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

  it('rejects legacy real-time interactive tool limits', () => {
    const yaml = `version: 2
hack: false
mode:
  type: interactive
  roles: [main]
  interactor:
    path: tools/interactor.cpp
    limits:
      realTimeMs: 1000
      memoryMb: 256
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
      - input: tests/1.in
        answer: tests/1.ans
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('mode.interactor.limits.timeMs must be an integer.')
    }
  })

  it('rejects interactive tool declarations without limits', () => {
    const yaml = `version: 2
hack: false
mode:
  type: interactive
  roles: [main]
  interactor:
    path: tools/interactor.cpp
strategyProvider: tools/strategy.cpp
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
      - input: tests/1.in
        answer: tests/1.ans
`

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('mode.interactor.limits is required and must be an object.')
      expect(result.errors).toContain('strategyProvider must be an object with path and limits.')
    }
  })

  it('rejects invalid checker declarations and missing checker files', () => {
    const invalidBuiltin = validateJudgeConfigYaml(judgeConfigTemplate.replace('name: exact', 'name: fuzzy'), templateFiles)
    expect(invalidBuiltin.ok).toBe(false)

    const missingCpp = validateJudgeConfigYaml(
      judgeConfigTemplate.replace(
        `type: builtin
  name: exact`,
        `type: cpp17
  path: checker/main.cpp`,
      ),
      templateFiles,
    )

    expect(missingCpp.ok).toBe(false)
    if (!missingCpp.ok) {
      expect(missingCpp.errors).toContain('checker.path does not exist: checker/main.cpp.')
    }
  })
})
