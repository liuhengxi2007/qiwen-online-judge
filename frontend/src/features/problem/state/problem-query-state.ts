import type { ProblemDetail } from '@/features/problem/model/response/ProblemDetail'
import type { ProblemSummary } from '@/features/problem/model/response/ProblemSummary'

export type ProblemPageState = {
  problems: ProblemSummary[]
  isLoading: boolean
  errorMessage: string
}

export type ProblemPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problems: ProblemSummary[] }
  | { type: 'load_failed'; message: string }

export const initialProblemPageState: ProblemPageState = {
  problems: [],
  isLoading: true,
  errorMessage: '',
}

export function reduceProblemPageState(
  state: ProblemPageState,
  action: ProblemPageAction,
): ProblemPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, problems: action.problems, errorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, errorMessage: action.message }
  }
}

export type ProblemDetailQueryState = {
  problem: ProblemDetail | null
  isLoading: boolean
  errorMessage: string
}

export type ProblemDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problem: ProblemDetail }
  | { type: 'replace'; problem: ProblemDetail }
  | { type: 'load_failed'; message: string }

export const initialProblemDetailQueryState: ProblemDetailQueryState = {
  problem: null,
  isLoading: true,
  errorMessage: '',
}

export function reduceProblemDetailQueryState(
  state: ProblemDetailQueryState,
  action: ProblemDetailQueryAction,
): ProblemDetailQueryState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { problem: action.problem, isLoading: false, errorMessage: '' }
    case 'replace':
      return { problem: action.problem, isLoading: false, errorMessage: '' }
    case 'load_failed':
      return { problem: null, isLoading: false, errorMessage: action.message }
  }
}
