import { createContext } from 'react'
import type { FeedbackDialogRequest, MockDialogRequest, MockResultOption, NoticeTone } from './mock-types'

export interface MockSystemContextValue {
  openMockDialog: <TOption extends MockResultOption<any, any>>(request: MockDialogRequest<TOption>) => void
  openFeedbackDialog: (request: FeedbackDialogRequest) => void
  showNotice: (message: string, tone?: NoticeTone) => void
}

export const MockSystemContext = createContext<MockSystemContextValue | null>(null)
