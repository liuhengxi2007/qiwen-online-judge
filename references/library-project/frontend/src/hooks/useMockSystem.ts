import { useContext } from 'react'

import { MockSystemContext } from '@/lib/mock-system-context'

export function useMockSystem() {
  const context = useContext(MockSystemContext)
  if (!context) {
    throw new Error('useMockSystem must be used within MockSystemProvider')
  }
  return context
}
