/**
 * 二进制大小格式化选项，用于控制小值是否保留字节单位或至少提升到 KiB。
 */
type BinarySizeFormatOptions = {
  minimumUnit?: 'B' | 'KiB'
}

/**
 * 将字节数格式化为 B/KiB/MiB/GiB 文案；输入为数字字节值，输出用于页面展示。
 */
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

/**
 * 格式化可缺失的字节数；null 使用 fallback，非空时沿用二进制单位格式化规则。
 */
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

/**
 * 为 KiB 以上单位选择可读精度，小于 1 时固定两位小数，其余值最多保留三位有效数字。
 */
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
