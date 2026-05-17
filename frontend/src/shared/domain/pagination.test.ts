import { describe, expect, it } from 'vitest'

import {
  buildPageNumbers,
  calculateTotalPages,
  getPageCorrection,
  parsePositivePage,
} from '@/shared/domain/pagination'

describe('pagination', () => {
  it('parses positive integer page query values', () => {
    expect(parsePositivePage(null)).toBe(1)
    expect(parsePositivePage('')).toBe(1)
    expect(parsePositivePage('0')).toBe(1)
    expect(parsePositivePage('-2')).toBe(1)
    expect(parsePositivePage('1.5')).toBe(1)
    expect(parsePositivePage('3')).toBe(3)
  })

  it('calculates at least one total page', () => {
    expect(calculateTotalPages(0, 10)).toBe(1)
    expect(calculateTotalPages(1, 10)).toBe(1)
    expect(calculateTotalPages(10, 10)).toBe(1)
    expect(calculateTotalPages(11, 10)).toBe(2)
  })

  it('builds a bounded page number window around the current page', () => {
    expect(buildPageNumbers(1, 10)).toEqual([1, 2, 3])
    expect(buildPageNumbers(5, 10)).toEqual([3, 4, 5, 6, 7])
    expect(buildPageNumbers(10, 10)).toEqual([8, 9, 10])
  })

  it('decides how to correct out-of-range pages', () => {
    expect(getPageCorrection(2, 5)).toEqual({ kind: 'none' })
    expect(getPageCorrection(2, 1)).toEqual({ kind: 'delete' })
    expect(getPageCorrection(9, 5)).toEqual({ kind: 'set', page: 5 })
  })
})
