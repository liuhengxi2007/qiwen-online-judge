import { codeRolePattern, textRolePattern } from './constants'
import type { ModeConfig } from './objects/JudgeConfigMode'
import type { ValidationContext } from './objects/JudgeConfigValidation'
import { isRecord } from './scalarValidators'
import { validatePathValue } from './pathValidators'
import { validateLimitedTool } from './resourceValidators'

/**
 * 校验判题模式；支持 traditional 简写字符串和 interactive 对象模式。
 */
export function validateMode(value: unknown, label: string, ctx: ValidationContext): ModeConfig | null {
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
export function validateRoleConfigs(value: unknown, label: string, ctx: ValidationContext): void {
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
export function validateHeaders(value: unknown, label: string, ctx: ValidationContext): void {
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
export function validateOptionalRoleList(value: unknown, label: string, ctx: ValidationContext): string[] | null {
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
