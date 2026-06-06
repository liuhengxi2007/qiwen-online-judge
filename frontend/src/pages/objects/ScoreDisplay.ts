import type { CSSProperties } from 'react'

export function clampScoreRatio(ratio: number): number {
  if (!Number.isFinite(ratio)) {
    return 0
  }
  return Math.min(1, Math.max(0, ratio))
}

export function scoreHueForRatio(ratio: number): number {
  return Math.round(115 * clampScoreRatio(ratio))
}

export function scoreTextStyleForRatio(ratio: number): CSSProperties {
  return {
    color: scoreTextColorForRatio(ratio),
  }
}

export function scorePillStyleForRatio(ratio: number): CSSProperties {
  const hue = scoreHueForRatio(ratio)
  return {
    backgroundColor: `hsla(${hue}, 82%, 92%, 0.9)`,
    color: `hsl(${hue}, 72%, 34%)`,
  }
}

function scoreTextColorForRatio(ratio: number): string {
  return `hsl(${scoreHueForRatio(ratio)}, 72%, 34%)`
}
