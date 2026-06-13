import { parseDocument } from 'yaml'

import { parseProblemDataPath, problemDataPathValue } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNode } from '@/objects/problem/response/ProblemDataTreeNode'

/**
 * 题目数据中判题配置文件的固定路径。
 */
export const judgeConfigPath = 'judge.yaml' as const

/**
 * 新建题目数据时提供的 judge.yaml v2 模板，包含传统题、限制、checker 和示例测试点。
 */
export const judgeConfigTemplate = `version: 2
hack: false
roundingScale: 6

mode:
  type: traditional
  role: main

limits:
  timeMs: 1000
  memoryMb: 256

checker:
  type: builtin
  name: exact

aggregation:
  testcases: sum_max_max
  subtasks: sum_max_max

subtasks:
  - label: main
    testcases:
      - label: sample-1
        type: sample
        input: sample/1.in
        answer: sample/1.ans

      - label: "1"
        input: tests/1.in
        answer: tests/1.ans
`

/**
 * judge.yaml v2 支持的聚合策略集合，用于校验 testcase/subtask aggregation 字段。
 */
const aggregations = new Set(['min_max_max', 'min_sum_max', 'sum_max_max', 'sum_sum_max'])
/**
 * 测试点类型白名单，区分主测试点、样例和 Hack 专用数据。
 */
const testcaseTypes = new Set(['main', 'sample', 'hack'])
/**
 * 代码角色名格式，只允许 ASCII 字母、数字、下划线和连字符。
 */
const codeRolePattern = /^[A-Za-z0-9_-]+$/
/**
 * 文本角色名格式，供传统模式 testcase.roles 兼容单个 .txt 后缀。
 */
const textRolePattern = /^[A-Za-z0-9_-]+\.txt$/

/**
 * 判题配置校验结果；成功仅携带 warning，失败同时返回错误列表和 warning。
 */
export type JudgeConfigValidationResult =
  | { ok: true; warnings: string[] }
  | { ok: false; errors: string[]; warnings: string[] }

/**
 * 判题配置校验上下文，累积错误并保存题目数据中已存在的文件路径集合。
 */
type ValidationContext = {
  errors: string[]
  filePaths: Set<string>
}

/**
 * 普通评测资源限制配置，单位与 judge.yaml 字段保持一致。
 */
type LimitsConfig = {
  timeMs: number
  memoryMb: number
}

/**
 * 工具程序资源限制配置，用于 interactor、strategyProvider 等受限工具。
 */
type ToolLimitsConfig = {
  timeMs: number
  memoryMb: number
}

/**
 * 工具程序配置，包含必需路径和可选资源限制。
 */
type ToolConfig = {
  path: string
  limits?: ToolLimitsConfig
}

/**
 * checker 配置，支持内置 exact/echo 和 C++17 自定义 checker。
 */
type CheckerConfig =
  | { type: 'builtin'; name: 'exact' }
  | { type: 'builtin'; name: 'echo' }
  | { type: 'cpp17'; path: string }

/**
 * 判题模式配置，区分传统单角色和交互式多角色。
 */
type ModeConfig =
  | { type: 'traditional'; role: string }
  | { type: 'interactive'; roles: string[]; interactor: ToolConfig }

/**
 * 聚合策略配置，允许根级和 subtask 级分别声明 testcase/subtask 聚合方式。
 */
type AggregationConfig = {
  testcases?: string
  subtasks?: string
}

/**
 * 标准答案生成配置，区分未声明、显式禁用和生成器文件。
 */
type StandardConfig =
  | { type: 'unspecified' }
  | { type: 'none' }
  | { type: 'generator'; path: string }

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
  validateOptionalInteger(root.roundingScale, 'roundingScale', 0, 18, ctx)
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
      validateHackCapability(subtaskLabel, hack, validator, standard, ctx)
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

/**
 * 校验 limits 对象；未声明返回 null，声明时要求 timeMs 和 memoryMb 都在允许范围内。
 */
function validateLimits(value: unknown, label: string, ctx: ValidationContext): LimitsConfig | null {
  if (value === undefined) {
    return null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object.`)
    return null
  }

  const timeMs = validateIntegerRange(value.timeMs, `${label}.timeMs`, 1, 600000, ctx)
  const memoryMb = validateIntegerRange(value.memoryMb, `${label}.memoryMb`, 1, 65536, ctx)
  return timeMs !== null && memoryMb !== null ? { timeMs, memoryMb } : null
}

/**
 * 校验 checker 配置；支持内置 checker 和 cpp17/cpp 自定义 checker 文件路径。
 */
function validateChecker(value: unknown, label: string, ctx: ValidationContext): CheckerConfig | null {
  if (value === undefined) {
    return null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object.`)
    return null
  }

  if (value.type === 'builtin') {
    if (value.name === 'exact' || value.name === 'echo') {
      return { type: 'builtin', name: value.name }
    }
    ctx.errors.push(`${label}.name must be exact or echo for builtin checker.`)
    return null
  }

  if (value.type === 'cpp17' || value.type === 'cpp') {
    const path = validatePathValue(value.path, `${label}.path`, ctx)
    return path ? { type: 'cpp17', path } : null
  }

  ctx.errors.push(`${label}.type must be builtin or cpp17.`)
  return null
}

/**
 * 校验不带资源限制的工具配置；允许简写为路径字符串或对象 path 字段。
 */
function validateTool(value: unknown, label: string, ctx: ValidationContext): ToolConfig | null {
  if (value === undefined) {
    return null
  }
  if (typeof value === 'string') {
    const path = validatePathValue(value, label, ctx)
    return path ? { path } : null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be a path string or an object with a path.`)
    return null
  }
  const path = validatePathValue(value.path, `${label}.path`, ctx)
  return path ? { path } : null
}

/**
 * 校验带资源限制的工具配置；必须声明 path 和 limits。
 */
function validateLimitedTool(value: unknown, label: string, ctx: ValidationContext): ToolConfig | null {
  if (value === undefined) {
    return null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object with path and limits.`)
    return null
  }

  const path = validatePathValue(value.path, `${label}.path`, ctx)
  const limits = validateToolLimits(value.limits, `${label}.limits`, ctx)
  return path && limits ? { path, limits } : null
}

/**
 * 校验 standard 配置；undefined 表示继承父级，false 表示禁用，对象表示答案生成器。
 */
function validateStandard(value: unknown, label: string, ctx: ValidationContext): StandardConfig {
  if (value === undefined) {
    return { type: 'unspecified' }
  }
  if (value === false) {
    return { type: 'none' }
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object or false.`)
    return { type: 'unspecified' }
  }

  if (value.language !== 'cpp17' && value.language !== 'python3') {
    ctx.errors.push(`${label}.language must be cpp17 or python3.`)
  }
  const path = validatePathValue(value.path, `${label}.path`, ctx)
  return path ? { type: 'generator', path } : { type: 'unspecified' }
}

/**
 * 计算 standard 继承结果；子级未声明时使用父级配置。
 */
function inheritStandard(value: StandardConfig, parent: StandardConfig): StandardConfig {
  return value.type === 'unspecified' ? parent : value
}

/**
 * 校验 Hack 开启时的必要依赖，要求 validator 和 standard 策略都明确存在。
 */
function validateHackCapability(
  label: string,
  enabled: boolean,
  validator: ToolConfig | null,
  standard: StandardConfig,
  ctx: ValidationContext,
): void {
  if (!enabled) {
    return
  }
  if (!validator) {
    ctx.errors.push(`Validator is required for ${label} when hack is enabled.`)
  }
  if (standard.type === 'unspecified') {
    ctx.errors.push(`standard must be declared as an answer generator object or false for ${label} when hack is enabled.`)
  }
}

/**
 * 校验工具 limits 对象；工具限制必须同时声明合法的 timeMs 和 memoryMb。
 */
function validateToolLimits(value: unknown, label: string, ctx: ValidationContext): ToolLimitsConfig | null {
  if (!isRecord(value)) {
    ctx.errors.push(`${label} is required and must be an object.`)
    return null
  }

  const timeMs = validateIntegerRange(value.timeMs, `${label}.timeMs`, 1, 600000, ctx)
  const memoryMb = validateIntegerRange(value.memoryMb, `${label}.memoryMb`, 1, 65536, ctx)
  return timeMs !== null && memoryMb !== null ? { timeMs, memoryMb } : null
}

/**
 * 校验判题模式；支持 traditional 简写字符串和 interactive 对象模式。
 */
function validateMode(value: unknown, label: string, ctx: ValidationContext): ModeConfig | null {
  if (value === undefined) {
    return null
  }
  if (typeof value === 'string') {
    if (value === 'traditional') {
      return { type: 'traditional', role: 'main' }
    }
    ctx.errors.push(`${label} must be an object for interactive mode.`)
    return null
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be a string or an object.`)
    return null
  }

  if (value.type === 'traditional') {
    const role = validateRole(typeof value.role === 'string' ? value.role : 'main', `${label}.role`, ctx, { allowTextRole: true })
    return role ? { type: 'traditional', role } : null
  }

  if (value.type === 'interactive') {
    const roles = validateRoleList(value.roles, `${label}.roles`, ctx, { allowTextRole: false })
    const interactor = validateLimitedTool(value.interactor, `${label}.interactor`, ctx)
    if (value.interactor === undefined) {
      ctx.errors.push(`${label}.interactor is required.`)
    }
    return roles && interactor ? { type: 'interactive', roles, interactor } : null
  }

  ctx.errors.push(`${label}.type must be traditional or interactive.`)
  return null
}

/**
 * 校验 roles 配置对象，逐个校验角色名和对应 stub 配置。
 */
function validateRoleConfigs(value: unknown, label: string, ctx: ValidationContext): void {
  if (value === undefined) {
    return
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object.`)
    return
  }

  Object.entries(value).forEach(([role, config]) => {
    const parsedRole = validateRole(role, `${label}.${role}`, ctx, { allowTextRole: false })
    if (!isRecord(config)) {
      ctx.errors.push(`${label}.${role} must be an object.`)
      return
    }
    if (parsedRole) {
      validateRoleStubs(config.stubs, `${label}.${role}.stubs`, ctx)
    }
  })
}

/**
 * 校验 headers 列表，要求每个引用存在、以 .h 结尾且 include 文件名不重复。
 */
function validateHeaders(value: unknown, label: string, ctx: ValidationContext): void {
  if (value === undefined) {
    return
  }
  if (!Array.isArray(value)) {
    ctx.errors.push(`${label} must be a list.`)
    return
  }

  const includeNames = new Set<string>()
  value.forEach((item, index) => {
    const path = validatePathValue(item, `${label}[${index}]`, ctx)
    if (!path) {
      return
    }

    if (!path.toLowerCase().endsWith('.h')) {
      ctx.errors.push(`${label}[${index}] must end with .h.`)
    }

    const includeName = path.split('/').pop() ?? path
    if (includeNames.has(includeName)) {
      ctx.errors.push(`${label}[${index}] duplicates include name ${includeName}.`)
    }
    includeNames.add(includeName)
  })
}

/**
 * 校验交互角色 stub 配置，目前只支持 cpp17 stub 文件路径。
 */
function validateRoleStubs(value: unknown, label: string, ctx: ValidationContext): void {
  if (value === undefined) {
    return
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object.`)
    return
  }

  Object.entries(value).forEach(([language, path]) => {
    if (language !== 'cpp17') {
      ctx.errors.push(`${label}.${language} is not supported. Only cpp17 stubs are supported.`)
      return
    }
    validatePathValue(path, `${label}.${language}`, ctx)
  })
}

/**
 * 校验必填角色列表，返回合法角色名数组，列表为空或非法时记录错误。
 */
function validateRoleList(value: unknown, label: string, ctx: ValidationContext, options: { allowTextRole: boolean }): string[] | null {
  if (!Array.isArray(value)) {
    ctx.errors.push(`${label} is required and must be a list.`)
    return null
  }
  const roles = value.flatMap((item, index) => {
    if (typeof item !== 'string') {
      ctx.errors.push(`${label}[${index}] must be a string.`)
      return []
    }
    const role = validateRole(item, `${label}[${index}]`, ctx, options)
    return role ? [role] : []
  })
  if (roles.length === 0) {
    ctx.errors.push(`${label} must contain at least one role.`)
  }
  return roles
}

/**
 * 校验可选角色列表；未声明返回 null，声明时必须是非空列表。
 */
function validateOptionalRoleList(value: unknown, label: string, ctx: ValidationContext): string[] | null {
  if (value === undefined) {
    return null
  }
  if (!Array.isArray(value)) {
    ctx.errors.push(`${label} must be a list.`)
    return null
  }
  const roles = value.flatMap((item, index) => {
    if (typeof item !== 'string') {
      ctx.errors.push(`${label}[${index}] must be a string.`)
      return []
    }
    const role = validateRole(item, `${label}[${index}]`, ctx, { allowTextRole: true })
    return role ? [role] : []
  })
  if (roles.length === 0) {
    ctx.errors.push(`${label} must contain at least one role.`)
  }
  return roles
}

/**
 * 校验单个角色名格式，可按调用场景允许传统模式的 .txt 文本角色。
 */
function validateRole(value: string, label: string, ctx: ValidationContext, options: { allowTextRole: boolean }): string | null {
  const role = value.trim()
  const isValidRole = codeRolePattern.test(role) || (options.allowTextRole && textRolePattern.test(role))
  if (!role || !isValidRole) {
    const suffix = options.allowTextRole ? ', with an optional single ".txt" suffix' : ''
    ctx.errors.push(`${label} must contain only ASCII letters, digits, "_" or "-"${suffix}.`)
    return null
  }
  return role
}

/**
 * 校验 aggregation 对象，分别读取 testcase 和 subtask 聚合策略。
 */
function validateAggregation(value: unknown, label: string, ctx: ValidationContext): AggregationConfig {
  if (value === undefined) {
    return {}
  }
  if (!isRecord(value)) {
    ctx.errors.push(`${label} must be an object.`)
    return {}
  }

  return {
    testcases: validateAggregationValue(value.testcases, `${label}.testcases`, ctx),
    subtasks: validateAggregationValue(value.subtasks, `${label}.subtasks`, ctx),
  }
}

/**
 * 校验单个聚合策略值，缺失时返回 undefined 以便后续继承父级。
 */
function validateAggregationValue(value: unknown, label: string, ctx: ValidationContext): string | undefined {
  if (value === undefined) {
    return undefined
  }
  if (typeof value !== 'string' || !aggregations.has(value)) {
    ctx.errors.push(`${label} must be one of: ${Array.from(aggregations).join(', ')}.`)
    return undefined
  }
  return value
}

/**
 * 校验测试点类型；缺失时按 main 处理，非法值记录错误并返回 null。
 */
function validateTestcaseType(value: unknown, label: string, ctx: ValidationContext): 'main' | 'sample' | 'hack' | null {
  if (value === undefined) {
    return 'main'
  }
  if (typeof value !== 'string' || !testcaseTypes.has(value)) {
    ctx.errors.push(`${label} must be one of: main, sample, hack.`)
    return null
  }
  return value as 'main' | 'sample' | 'hack'
}

/**
 * 合并父级和子级聚合策略，子级声明优先。
 */
function mergeAggregation(parent: AggregationConfig, child: AggregationConfig): AggregationConfig {
  return {
    testcases: child.testcases ?? parent.testcases,
    subtasks: child.subtasks ?? parent.subtasks,
  }
}

/**
 * 带原始下标的 YAML 节点值，用于错误消息指向列表位置。
 */
type IndexedValue = {
  value: unknown
  index: number
}

/**
 * 十进制数字的整数化表示，用于无浮点误差地累加 scoreRatio。
 */
type DecimalUnits = {
  units: bigint
  scale: number
}

/**
 * 校验同级列表中的 scoreRatio 总和是否超过 1。
 */
function validateSiblingRatios(items: unknown[], label: string, ctx: ValidationContext): void {
  validateSiblingRatioEntries(items.map((value, index) => ({ value, index })), label, ctx)
}

/**
 * 校验带索引的 scoreRatio 条目，忽略未声明比例的条目并累加显式比例。
 */
function validateSiblingRatioEntries(items: IndexedValue[], label: string, ctx: ValidationContext): void {
  const explicitRatios: DecimalUnits[] = []

  items.forEach(({ value: item, index }) => {
    if (!isRecord(item) || item.scoreRatio === undefined) {
      return
    }
    if (typeof item.scoreRatio !== 'number' || !Number.isFinite(item.scoreRatio)) {
      ctx.errors.push(`${label}[${index}].scoreRatio must be a number.`)
      return
    }
    if (item.scoreRatio < 0 || item.scoreRatio > 1) {
      ctx.errors.push(`${label}[${index}].scoreRatio must be between 0 and 1.`)
      return
    }
    explicitRatios.push(decimalUnitsFromNumber(item.scoreRatio))
  })

  if (decimalUnitsExceedOne(explicitRatios)) {
    ctx.errors.push(`${label} explicit scoreRatio values must not sum above 1.`)
  }
}

/**
 * 将 JS number 转为十进制整数和缩放位数，支持科学计数法输入。
 */
function decimalUnitsFromNumber(value: number): DecimalUnits {
  const [coefficient, exponentPart] = value.toString().toLowerCase().split('e')
  const exponent = exponentPart === undefined ? 0 : Number(exponentPart)
  const [wholePart, fractionPart = ''] = coefficient.split('.')
  const digits = `${wholePart}${fractionPart}` || '0'
  const scale = fractionPart.length - exponent

  if (scale <= 0) {
    return { units: BigInt(digits) * pow10(-scale), scale: 0 }
  }
  return { units: BigInt(digits), scale }
}

/**
 * 在统一小数位后判断多个十进制比例之和是否超过 1。
 */
function decimalUnitsExceedOne(values: DecimalUnits[]): boolean {
  const scale = Math.max(0, ...values.map((value) => value.scale))
  const sum = values.reduce((total, value) => total + value.units * pow10(scale - value.scale), 0n)
  return sum > pow10(scale)
}

/**
 * 返回 10 的 BigInt 幂，用于十进制比例缩放。
 */
function pow10(exponent: number): bigint {
  return 10n ** BigInt(exponent)
}

/**
 * 校验必填文件引用，要求非空字符串且路径存在于题目数据树。
 */
function validateRequiredFileRef(value: unknown, label: string, ctx: ValidationContext): void {
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} is required.`)
    return
  }
  validateExistingFile(value, label, ctx)
}

/**
 * 校验可选文件引用；缺失时跳过，存在时要求非空且路径有效。
 */
function validateOptionalFileRef(value: unknown, label: string, ctx: ValidationContext): void {
  if (value === undefined) {
    return
  }
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} must be a nonempty string when present.`)
    return
  }
  validateExistingFile(value, label, ctx)
}

/**
 * 校验路径字段，返回规范化后的题目数据路径或记录错误。
 */
function validatePathValue(value: unknown, label: string, ctx: ValidationContext): string | null {
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} is required.`)
    return null
  }
  return validateExistingFile(value, label, ctx)
}

/**
 * 校验文件路径语法并确认文件存在于当前数据树。
 */
function validateExistingFile(rawPath: string, label: string, ctx: ValidationContext): string | null {
  const parsedPath = parseProblemDataPath(rawPath)
  if (!parsedPath.ok) {
    ctx.errors.push(`${label} is invalid: ${parsedPath.error}`)
    return null
  }

  const path = problemDataPathValue(parsedPath.value)
  if (!ctx.filePaths.has(path)) {
    ctx.errors.push(`${label} does not exist: ${path}.`)
    return null
  }
  return path
}

/**
 * 拒绝 v1 遗留 name 字段，引导用户在 judge.yaml v2 中使用 label。
 */
function rejectLegacyName(value: Record<string, unknown>, label: string, ctx: ValidationContext): void {
  if (value.name !== undefined) {
    ctx.errors.push(`${label}.name is not supported in judge.yaml v2; use label instead.`)
  }
}

/**
 * 校验可选 label 字段，存在时必须是非空字符串。
 */
function validateOptionalLabel(value: unknown, label: string, ctx: ValidationContext): void {
  if (value === undefined) {
    return
  }
  if (typeof value !== 'string' || value.trim() === '') {
    ctx.errors.push(`${label} must be a nonempty string when present.`)
  }
}

/**
 * 校验数字字段是否等于指定版本或常量值。
 */
function requireExactNumber(value: unknown, label: string, expected: number, ctx: ValidationContext): void {
  if (value !== expected) {
    ctx.errors.push(`${label} must be ${expected}.`)
  }
}

/**
 * 校验可选整数范围；缺失时跳过，存在时必须为指定闭区间内整数。
 */
function validateOptionalInteger(
  value: unknown,
  label: string,
  min: number,
  max: number,
  ctx: ValidationContext,
): void {
  if (value === undefined) {
    return
  }
  if (typeof value !== 'number' || !Number.isInteger(value)) {
    ctx.errors.push(`${label} must be an integer.`)
    return
  }
  if (value < min || value > max) {
    ctx.errors.push(`${label} must be between ${min} and ${max}.`)
  }
}

/**
 * 校验可选布尔字段；缺失返回 null，非法类型记录错误。
 */
function validateOptionalBoolean(value: unknown, label: string, ctx: ValidationContext): boolean | null {
  if (value === undefined) {
    return null
  }
  if (typeof value !== 'boolean') {
    ctx.errors.push(`${label} must be a boolean.`)
    return null
  }
  return value
}

/**
 * 校验必填整数范围，非法时记录错误并返回 null。
 */
function validateIntegerRange(
  value: unknown,
  label: string,
  min: number,
  max: number,
  ctx: ValidationContext,
): number | null {
  if (typeof value !== 'number' || !Number.isInteger(value)) {
    ctx.errors.push(`${label} must be an integer.`)
    return null
  }
  if (value < min || value > max) {
    ctx.errors.push(`${label} must be between ${min} and ${max}.`)
    return null
  }
  return value
}

/**
 * 校验必填列表字段，非法时记录错误并返回 null。
 */
function validateList(value: unknown, label: string, ctx: ValidationContext): unknown[] | null {
  if (!Array.isArray(value)) {
    ctx.errors.push(`${label} is required and must be a list.`)
    return null
  }
  return value
}

/**
 * 生成用于错误消息的 subtask/testcase 标签，优先包含用户声明的 label。
 */
function judgeNodeLabel(kind: 'subtask' | 'testcase', index: number, label: unknown): string {
  return typeof label === 'string' && label.trim() ? `${kind} ${index} (${label.trim()})` : `${kind} ${index}`
}

/**
 * 将未知 YAML 节点收窄为普通对象，排除 null 和数组。
 */
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

/**
 * 根据上下文错误集合构造最终校验结果；当前校验器暂不产生 warning。
 */
function toResult(ctx: ValidationContext): JudgeConfigValidationResult {
  return ctx.errors.length > 0 ? { ok: false, errors: ctx.errors, warnings: [] } : { ok: true, warnings: [] }
}
