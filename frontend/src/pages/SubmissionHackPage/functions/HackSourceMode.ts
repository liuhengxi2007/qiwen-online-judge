export type HackSourceMode = 'paste' | 'file'

export type HackSourceDraft = {
  mode: HackSourceMode
  text: string
  file: File | null
}

export function hasHackSourceContent(source: HackSourceDraft): boolean {
  return source.mode === 'file' ? source.file !== null : source.text.length > 0
}

export function canSubmitHackSources(
  input: HackSourceDraft,
  strategyProvider: HackSourceDraft,
  requiresStrategyProvider: boolean,
): boolean {
  return hasHackSourceContent(input) && (!requiresStrategyProvider || hasHackSourceContent(strategyProvider))
}

export function usesHackMultipart(
  input: HackSourceDraft,
  strategyProvider: HackSourceDraft,
  requiresStrategyProvider: boolean,
): boolean {
  return input.mode === 'file' || (requiresStrategyProvider && strategyProvider.mode === 'file')
}
