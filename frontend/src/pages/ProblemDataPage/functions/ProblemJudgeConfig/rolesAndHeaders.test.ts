import { describe, expect, it } from 'vitest'

import { validateJudgeConfigYaml } from '../ProblemJudgeConfig'
import { templateFiles } from './testHelpers'

describe('problem-judge-config roles and headers', () => {
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

  it('accepts declared cpp headers', () => {
    const yaml = `version: 2
hack: false
headers:
  - headers/xxx.h
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

  it('rejects invalid header declarations', () => {
    const result = validateJudgeConfigYaml(
      `version: 2
hack: false
headers:
  - headers/missing.h
  - headers/readme.txt
  - headers/xxx.h
  - headers/other/xxx.h
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
      expect(result.errors).toContain('headers[0] does not exist: headers/missing.h.')
      expect(result.errors).toContain('headers[1] must end with .h.')
      expect(result.errors).toContain('headers[3] duplicates include name xxx.h.')
    }
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
})
