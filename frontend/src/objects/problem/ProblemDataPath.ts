/** 题目数据相对路径品牌类型；禁止绝对路径、空路径和目录穿越片段。 */
export type ProblemDataPath = string & { readonly __brand: 'ProblemDataPath' }

/** 解析 helper 的返回结果；成功携带品牌值，失败携带可展示错误，不抛异常。 */
type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

/** 创建题目数据路径品牌值；调用前必须完成路径安全校验。 */
function createProblemDataPath(value: string): ProblemDataPath {
  /** 注意：这里的 as 只在 parseProblemDataPath 校验通过后施加品牌类型。 */
  return value as ProblemDataPath
}

/** 将题目数据路径还原为字符串；用于下载、删除和上传接口。 */
export function problemDataPathValue(path: ProblemDataPath): string {
  return path
}

/** 解析题目数据路径；会统一反斜杠并拒绝目录穿越，不访问文件系统。 */
export function parseProblemDataPath(rawPath: string): ParseResult<ProblemDataPath> {
  const normalized = rawPath.trim().replaceAll('\\', '/')
  if (!normalized) {
    return { ok: false, error: 'Problem data path is required.' }
  }
  if (normalized.length > 1024) {
    return { ok: false, error: 'Problem data path must be at most 1024 characters.' }
  }
  if (normalized.startsWith('/') || normalized.endsWith('/')) {
    return { ok: false, error: "Problem data path must be relative and must not start or end with '/'." }
  }
  const segments = normalized.split('/')
  if (segments.some((segment) => !segment)) {
    return { ok: false, error: 'Problem data path must not contain empty segments.' }
  }
  if (segments.some((segment) => segment === '.' || segment === '..')) {
    return { ok: false, error: "Problem data path must not contain '.' or '..' segments." }
  }
  if (segments.some((segment) => segment.length > 255)) {
    return { ok: false, error: 'Each problem data path segment must be at most 255 characters.' }
  }
  return { ok: true, value: createProblemDataPath(normalized) }
}
