import { describe, expect, it } from 'vitest'

import { buildAncestorLinks } from './AncestorNavigationLinks'

describe('buildAncestorLinks contest paths', () => {
  it('keeps contest list root mapping', () => {
    expect(buildAncestorLinks('/contests/sample')).toEqual([
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
    ])
  })

  it('maps contest registrants page to contest breadcrumb', () => {
    expect(buildAncestorLinks('/contests/sample/registrants')).toEqual([
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: '/contests/sample', labelKey: 'contests' },
    ])
  })

  it('maps contest ranklist page to contest breadcrumb with ranklist', () => {
    expect(buildAncestorLinks('/contests/sample/ranklist')).toEqual([
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: '/contests/sample', labelKey: 'contests' },
    ])
  })

  it('maps contest submissions page to contest breadcrumb with submissions', () => {
    expect(buildAncestorLinks('/contests/sample/submissions')).toEqual([
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: '/contests/sample', labelKey: 'contests' },
    ])
  })

  it('maps contest manage page to contest breadcrumb', () => {
    expect(buildAncestorLinks('/contests/sample/manage')).toEqual([
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: '/contests/sample', labelKey: 'contests' },
    ])
  })

  it('maps contest problem page to contest and problem breadcrumb', () => {
    expect(buildAncestorLinks('/contests/sample/problems/a')).toEqual([
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: '/contests/sample', labelKey: 'contests' },
    ])
  })

  it('maps contest problem submit page to contest and problem breadcrumb', () => {
    expect(buildAncestorLinks('/contests/sample/problems/a/submit')).toEqual([
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: '/contests/sample', labelKey: 'contests' },
      { to: '/contests/sample/problems/a', labelKey: 'problem' },
    ])
  })

  it('maps contest problem data page to contest and problem breadcrumb', () => {
    expect(buildAncestorLinks('/contests/sample/problems/a/data')).toEqual([
      { to: '/', labelKey: 'dashboard' },
      { to: '/contests', labelKey: 'contests' },
      { to: '/contests/sample', labelKey: 'contests' },
      { to: '/contests/sample/problems/a', labelKey: 'problem' },
    ])
  })
})
