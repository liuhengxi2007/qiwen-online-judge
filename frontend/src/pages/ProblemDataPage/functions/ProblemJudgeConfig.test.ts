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
  file('validators/validator.cpp'),
  file('solutions/std.cpp'),
  file('tools/interactor.cpp'),
  file('tools/strategy.cpp'),
  file('stubs/main.cpp'),
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

  it('accepts traditional testcase role fallback list with text role', () => {
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
      - roles: [chain.txt, main]
        input: tests/1.in
        answer: tests/1.ans
`

    expect(validateJudgeConfigYaml(yaml, templateFiles).ok).toBe(true)
  })

  it('accepts cpp17 role stubs', () => {
    const yaml = `version: 2
hack: false
roles:
  main:
    stubs:
      cpp17: stubs/main.cpp
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

  it('rejects invalid role stub declarations', () => {
    const result = validateJudgeConfigYaml(
      `version: 2
hack: false
roles:
  chain.txt:
    stubs:
      cpp17: stubs/main.cpp
  helper:
    stubs:
      python3: stubs/main.py
      cpp17: stubs/missing.cpp
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

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('roles.chain.txt must contain only ASCII letters, digits, "_" or "-".')
      expect(result.errors).toContain('roles.helper.stubs.python3 is not supported. Only cpp17 stubs are supported.')
      expect(result.errors).toContain('roles.helper.stubs.cpp17 does not exist: stubs/missing.cpp.')
    }
  })

  it('rejects text roles in interactive role lists', () => {
    const yaml = `version: 2
hack: false
mode:
  type: interactive
  roles: [chain.txt]
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

    const result = validateJudgeConfigYaml(yaml, templateFiles)

    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.errors).toContain('mode.roles[0] must contain only ASCII letters, digits, "_" or "-".')
    }
  })

  it('rejects invalid testcase text roles and interactive testcase roles', () => {
    const invalidTextRole = validateJudgeConfigYaml(
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
  - testcases:
      - roles: [a.b.txt]
        input: tests/1.in
        answer: tests/1.ans
`,
      templateFiles,
    )

    expect(invalidTextRole.ok).toBe(false)
    if (!invalidTextRole.ok) {
      expect(invalidTextRole.errors).toContain('subtask 1 testcase 1.roles[0] must contain only ASCII letters, digits, "_" or "-", with an optional single ".txt" suffix.')
    }

    const interactiveTestcaseRoles = validateJudgeConfigYaml(
      `version: 2
hack: false
mode:
  type: interactive
  roles: [main]
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
      - roles: [main]
        input: tests/1.in
        answer: tests/1.ans
`,
      templateFiles,
    )

    expect(interactiveTestcaseRoles.ok).toBe(false)
    if (!interactiveTestcaseRoles.ok) {
      expect(interactiveTestcaseRoles.errors).toContain('subtask 1 testcase 1.roles cannot be declared when mode is interactive.')
    }
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
