import type { CSSProperties } from 'react'

/**
 * 将分数比例限制在 0 到 1 之间，非法数字按 0 处理。
 */
export function clampScoreRatio(ratio: number): number {
  if (!Number.isFinite(ratio)) {
    return 0
  }
  return Math.min(1, Math.max(0, ratio))
}

/**
 * 将分数比例映射到色相值，低分偏红，高分偏绿。
 */
export function scoreHueForRatio(ratio: number): number {
  return Math.round(115 * clampScoreRatio(ratio))
}

/**
 * 为分数比例生成文字颜色样式。
 */
export function scoreTextStyleForRatio(ratio: number): CSSProperties {
  return {
    color: scoreTextColorForRatio(ratio),
  }
}

/**
 * 为分数比例生成胶囊背景和文字颜色样式。
 */
export function scorePillStyleForRatio(ratio: number): CSSProperties {
  const hue = scoreHueForRatio(ratio)
  return {
    backgroundColor: `hsla(${hue}, 82%, 92%, 0.9)`,
    color: `hsl(${hue}, 72%, 34%)`,
  }
}

/**
 * 根据分数比例生成 HSL 文字颜色字符串。
 */
function scoreTextColorForRatio(ratio: number): string {
  return `hsl(${scoreHueForRatio(ratio)}, 72%, 34%)`
}
