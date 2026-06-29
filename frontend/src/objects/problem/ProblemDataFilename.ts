/** 题目数据文件名品牌类型；用于单文件上传目标，调用前需校验非空和长度。 */
export type ProblemDataFilename = string & { readonly __brand: 'ProblemDataFilename' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建题目数据文件名品牌值；调用前必须完成校验。 */
function createProblemDataFilename(value: string): ProblemDataFilename {
  /** 注意：这里的 as 只在 parseProblemDataFilename 校验通过后施加品牌类型。 */
  return value as ProblemDataFilename
}

/** 将题目数据文件名还原为普通字符串；用于 multipart path 字段。 */
export function problemDataFilenameValue(filename: ProblemDataFilename): string {
  return filename
}

/** 解析题目数据文件名；拒绝空值和过长文件名，不检查远端文件是否存在。 */
export function parseProblemDataFilename(rawFilename: string): ParseResult<ProblemDataFilename> {
  const normalized = rawFilename.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem data file name is required.' }
  }
  if (normalized.length > 255) {
    return { ok: false, error: 'Problem data file name must be at most 255 characters.' }
  }
  return { ok: true, value: createProblemDataFilename(normalized) }
}
