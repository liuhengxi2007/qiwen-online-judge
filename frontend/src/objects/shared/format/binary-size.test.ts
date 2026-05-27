import { describe, expect, it } from 'vitest'

import { formatBinarySizeBytes, formatOptionalBinarySizeBytes } from '@/objects/shared/format/binary-size'

describe('binary size formatting', () => {
  it('formats byte values', () => {
    expect(formatBinarySizeBytes(512)).toBe('512 B')
  })

  it('formats KiB values', () => {
    expect(formatBinarySizeBytes(1024)).toBe('1 KiB')
    expect(formatBinarySizeBytes(1536)).toBe('1.5 KiB')
  })

  it('can keep KiB as the minimum displayed unit', () => {
    expect(formatBinarySizeBytes(0, { minimumUnit: 'KiB' })).toBe('0.00 KiB')
    expect(formatBinarySizeBytes(512, { minimumUnit: 'KiB' })).toBe('0.50 KiB')
  })

  it('formats MiB values', () => {
    expect(formatBinarySizeBytes(1024 * 1024)).toBe('1 MiB')
  })

  it('formats GiB values', () => {
    expect(formatBinarySizeBytes(1024 * 1024 * 1024)).toBe('1 GiB')
  })

  it('wraps nullable values with a fallback', () => {
    expect(formatOptionalBinarySizeBytes(null)).toBe('--')
    expect(formatOptionalBinarySizeBytes(null, 'Size unknown')).toBe('Size unknown')
    expect(formatOptionalBinarySizeBytes(1024)).toBe('1 KiB')
  })
})
