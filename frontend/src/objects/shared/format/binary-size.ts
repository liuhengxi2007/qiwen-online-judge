type BinarySizeFormatOptions = {
  minimumUnit?: 'B' | 'KiB'
}

export function formatBinarySizeBytes(value: number, options: BinarySizeFormatOptions = {}): string {
  if ((options.minimumUnit ?? 'B') === 'B' && value < 1000) {
    return `${new Intl.NumberFormat(undefined, {
      maximumFractionDigits: 0,
    }).format(value)} B`
  }

  const kib = value / 1024
  const mib = value / (1024 * 1024)
  const gib = value / (1024 * 1024 * 1024)

  if (kib < 1000) {
    return `${formatWithReadablePrecision(kib)} KiB`
  }

  if (mib < 1000) {
    return `${formatWithReadablePrecision(mib)} MiB`
  }

  return `${formatWithReadablePrecision(gib)} GiB`
}

export function formatOptionalBinarySizeBytes(
  value: number | null,
  fallback = '--',
  options: BinarySizeFormatOptions = {},
): string {
  if (value === null) {
    return fallback
  }

  return formatBinarySizeBytes(value, options)
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
