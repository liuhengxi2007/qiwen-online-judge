import { describe, expect, it } from 'vitest'

import { validateJudgeConfigYaml } from '../ProblemJudgeConfig'
import sharedFixtureRaw from '../../../../test/fixtures/judge-config-validation-cases.json?raw'
import { file } from './testHelpers'

type SharedJudgeConfigValidationFixture = {
  files: string[]
  cases: SharedJudgeConfigValidationCase[]
}

type SharedJudgeConfigValidationCase = {
  name: string
  valid: boolean
  yaml: string
}

const sharedFixture = JSON.parse(sharedFixtureRaw) as SharedJudgeConfigValidationFixture
const sharedFixtureFiles = sharedFixture.files.map(file)

describe('problem-judge-config shared fixture', () => {
  it.each(sharedFixture.cases)('matches shared validation fixture: $name', (testCase) => {
    expect(validateJudgeConfigYaml(testCase.yaml, sharedFixtureFiles).ok).toBe(testCase.valid)
  })
})
