export function formatOptionalDurationMs(value: number | null): string {
  if (value === null) {
    return '--'
  }

  return `${value} ms`
}

export function formatOptionalMemoryKb(value: number | null): string {
  if (value === null) {
    return '--'
  }

  const kib = value
  const mib = value / 1024
  const gib = value / (1024 * 1024)

  if (kib < 1000) {
    return `${formatWithReadablePrecision(kib)} KiB`
  }

  if (mib < 1000) {
    return `${formatWithReadablePrecision(mib)} MiB`
  }

  return `${formatWithReadablePrecision(gib)} GiB`
}

export function formatCodeLength(value: number): string {
  return `${value} B`
}

export function formatSubmissionDateTime(value: string): string {
  return new Date(value).toLocaleString()
}

function formatWithReadablePrecision(value: number): string {
  if (value < 1) {
    return new Intl.NumberFormat(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value)
  }

  return new Intl.NumberFormat(undefined, {
    maximumSignificantDigits: 3,
  }).format(value)
}
