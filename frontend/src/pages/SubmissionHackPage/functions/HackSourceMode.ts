/**
 * Hack 数据源输入模式，支持直接粘贴文本或上传文件。
 */
export type HackSourceMode = 'paste' | 'file'

/**
 * Hack 数据源草稿，保存当前模式、文本内容和选中的文件对象。
 */
export type HackSourceDraft = {
  mode: HackSourceMode
  text: string
  file: File | null
}

/**
 * 判断 Hack 数据源是否有可提交内容；文件模式要求存在文件，文本模式要求非空文本。
 */
export function hasHackSourceContent(source: HackSourceDraft): boolean {
  return source.mode === 'file' ? source.file !== null : source.text.length > 0
}

/**
 * 判断 Hack 表单是否可提交；输入数据必填，策略生成器在题目要求时也必须提供。
 */
export function canSubmitHackSources(
  input: HackSourceDraft,
  strategyProvider: HackSourceDraft,
  requiresStrategyProvider: boolean,
): boolean {
  return hasHackSourceContent(input) && (!requiresStrategyProvider || hasHackSourceContent(strategyProvider))
}

/**
 * 判断 Hack 提交是否需要 multipart；任一必需数据源使用文件模式时返回 true。
 */
export function usesHackMultipart(
  input: HackSourceDraft,
  strategyProvider: HackSourceDraft,
  requiresStrategyProvider: boolean,
): boolean {
  return input.mode === 'file' || (requiresStrategyProvider && strategyProvider.mode === 'file')
}
