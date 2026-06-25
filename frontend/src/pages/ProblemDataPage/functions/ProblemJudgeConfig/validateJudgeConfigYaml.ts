import { parseDocument } from 'yaml'

import { problemDataPathValue } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNode } from '@/objects/problem/response/ProblemDataTreeNode'

import type { IndexedValue, JudgeConfigValidationResult, ValidationContext } from './types'
import {
  inheritStandard,
  mergeAggregation,
  validateAggregation,
  validateChecker,
  validateHackCapability,
  validateHeaders,
  validateLimitedTool,
  validateLimits,
  validateMode,
  validateOptionalRoleList,
  validateRoleConfigs,
  validateSiblingRatioEntries,
  validateSiblingRatios,
  validateStandard,
  validateTestcaseType,
  validateTool,
} from './validators'
import {
  isRecord,
  judgeNodeLabel,
  rejectLegacyName,
  requireExactNumber,
  toResult,
  validateList,
  validateOptionalBoolean,
  validateOptionalFileRef,
  validateOptionalInteger,
  validateOptionalLabel,
  validateRequiredFileRef,
} from './utils'

/**
 * 校验 judge.yaml 内容和题目数据文件树；解析 YAML、验证 schema、继承默认配置并返回错误集合。
 */
export function validateJudgeConfigYaml(
  content: string,
  dataTree: ProblemDataTreeNode[],
): JudgeConfigValidationResult {
  // 注意：judge.yaml 来自用户可编辑文本，unknown/Record 校验是刻意设置的不可信输入边界。
  const document = parseDocument(content, { prettyErrors: false })
  const syntaxErrors = document.errors.map((error) => error.message)
  if (syntaxErrors.length > 0) {
    return { ok: false, errors: syntaxErrors, warnings: [] }
  }

  const root = document.toJSON()
  const ctx: ValidationContext = {
    errors: [],
    filePaths: new Set(
      dataTree
        .filter((node) => node.kind === 'file')
        .map((node) => problemDataPathValue(node.path)),
    ),
  }

  if (!isRecord(root)) {
    ctx.errors.push('judge.yaml must be an object.')
    return toResult(ctx)
  }

  requireExactNumber(root.version, 'version', 2, ctx)
  validateOptionalInteger({
    value: root.roundingScale,
    label: 'roundingScale',
    range: { min: 0, max: 18 },
    ctx,
  })
  validateHeaders(root.headers, 'headers', ctx)

  const rootLimits = validateLimits(root.limits, 'limits', ctx)
  const rootChecker = validateChecker(root.checker, 'checker', ctx)
  const rootValidator = validateTool(root.validator, 'validator', ctx)
  const rootStandard = validateStandard(root.standard, 'standard', ctx)
  const rootHack = validateOptionalBoolean(root.hack, 'hack', ctx) ?? true
  const rootMode = validateMode(root.mode, 'mode', ctx) ?? { type: 'traditional' as const, role: 'main' }
  validateRoleConfigs(root.roles, 'roles', ctx)
  validateLimitedTool(root.strategyProvider, 'strategyProvider', ctx)
  const rootAggregation = validateAggregation(root.aggregation, 'aggregation', ctx)
  const subtasks = validateList(root.subtasks, 'subtasks', ctx)
  if (subtasks && subtasks.length === 0) {
    ctx.errors.push('subtasks must contain at least one item.')
  }

  if (subtasks) {
    validateSiblingRatios(subtasks, 'subtasks', ctx)
    subtasks.forEach((subtask, index) => {
      if (!isRecord(subtask)) {
        ctx.errors.push(`subtasks[${index}] must be an object.`)
        return
      }

      const subtaskLabel = judgeNodeLabel('subtask', index + 1, subtask.label)
      rejectLegacyName(subtask, subtaskLabel, ctx)
      validateOptionalLabel(subtask.label, `subtasks[${index}].label`, ctx)
      const limits = validateLimits(subtask.limits, `${subtaskLabel}.limits`, ctx) ?? rootLimits
      const checker = validateChecker(subtask.checker, `${subtaskLabel}.checker`, ctx) ?? rootChecker
      const validator = validateTool(subtask.validator, `${subtaskLabel}.validator`, ctx) ?? rootValidator
      const standard = inheritStandard(validateStandard(subtask.standard, `${subtaskLabel}.standard`, ctx), rootStandard)
      const hack = validateOptionalBoolean(subtask.hack, `${subtaskLabel}.hack`, ctx) ?? rootHack
      validateHackCapability({
        label: subtaskLabel,
        enabled: hack,
        dependencies: { validator, standard },
        ctx,
      })
      const mode = validateMode(subtask.mode, `${subtaskLabel}.mode`, ctx) ?? rootMode
      validateLimitedTool(subtask.strategyProvider, `${subtaskLabel}.strategyProvider`, ctx)
      const aggregation = mergeAggregation(rootAggregation, validateAggregation(subtask.aggregation, `${subtaskLabel}.aggregation`, ctx))
      const testcases = validateList(subtask.testcases, `${subtaskLabel}.testcases`, ctx)

      if (testcases && testcases.length === 0) {
        ctx.errors.push(`${subtaskLabel}.testcases must contain at least one item.`)
      }
      if (testcases) {
        const mainTestcaseEntries: IndexedValue[] = []
        testcases.forEach((testcase, testcaseIndex) => {
          if (!isRecord(testcase)) {
            ctx.errors.push(`${subtaskLabel}.testcases[${testcaseIndex}] must be an object.`)
            return
          }

          const testcaseLabel = `${subtaskLabel} ${judgeNodeLabel('testcase', testcaseIndex + 1, testcase.label)}`
          const testcaseType = validateTestcaseType(testcase.type, `${testcaseLabel}.type`, ctx) ?? 'main'
          if (testcaseType === 'main') {
            mainTestcaseEntries.push({ value: testcase, index: testcaseIndex })
          }
          if (testcaseType !== 'main' && testcase.scoreRatio !== undefined) {
            ctx.errors.push(`${testcaseLabel}.scoreRatio cannot be declared when type is ${testcaseType}.`)
          }
          rejectLegacyName(testcase, testcaseLabel, ctx)
          validateOptionalLabel(testcase.label, `${testcaseLabel}.label`, ctx)
          if (testcase.mode !== undefined) {
            ctx.errors.push(`${testcaseLabel}.mode cannot be declared on a testcase.`)
          }
          if (testcase.validator !== undefined) {
            ctx.errors.push(`${testcaseLabel}.validator cannot be declared on a testcase.`)
          }
          if (testcase.hack !== undefined) {
            ctx.errors.push(`${testcaseLabel}.hack cannot be declared on a testcase.`)
          }
          if (mode.type === 'interactive' && testcase.roles !== undefined) {
            ctx.errors.push(`${testcaseLabel}.roles cannot be declared when mode is interactive.`)
          }
          if (mode.type === 'traditional') {
            validateOptionalRoleList(testcase.roles, `${testcaseLabel}.roles`, ctx)
          }
          const testcaseLimits = validateLimits(testcase.limits, `${testcaseLabel}.limits`, ctx) ?? limits
          const testcaseChecker = validateChecker(testcase.checker, `${testcaseLabel}.checker`, ctx) ?? checker
          if (testcase.strategyProvider !== undefined) {
            validateLimitedTool(testcase.strategyProvider, `${testcaseLabel}.strategyProvider`, ctx)
          }
          validateAggregation(testcase.aggregation, `${testcaseLabel}.aggregation`, ctx)

          if (!testcaseLimits) {
            ctx.errors.push(`Limits are required for ${testcaseLabel}.`)
          }
          if (!testcaseChecker) {
            ctx.errors.push(`Checker is required for ${testcaseLabel}.`)
          }
          if (!aggregation.testcases) {
            ctx.errors.push(`Testcase aggregation is required for ${subtaskLabel}.`)
          }
          if (mode.type === 'interactive' && mode.roles.length === 0) {
            ctx.errors.push(`${subtaskLabel}.mode.roles must contain at least one role.`)
          }

          validateRequiredFileRef(testcase.input, `${testcaseLabel}.input`, ctx)
          validateOptionalFileRef(testcase.answer, `${testcaseLabel}.answer`, ctx)
          if (testcaseType !== 'hack' && testcaseChecker?.type === 'builtin' && testcaseChecker.name === 'exact' && testcase.answer === undefined) {
            ctx.errors.push(`${testcaseLabel}.answer is required for builtin exact checker.`)
          }
        })
        if (mainTestcaseEntries.length === 0) {
          ctx.errors.push(`${subtaskLabel} must define at least one main testcase.`)
        }
        validateSiblingRatioEntries(mainTestcaseEntries, `${subtaskLabel}.testcases`, ctx)
      }
    })
  }

  return toResult(ctx)
}
