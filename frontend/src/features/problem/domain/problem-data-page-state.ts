import type { ProblemDataFilename, ProblemDataTreeNode } from '@/features/problem/domain/problem'

export type ProblemDataPageState = {
  isSavingReady: boolean
  selectedFile: File | null
  isUploading: boolean
  isLoadingFiles: boolean
  deletingFilename: ProblemDataFilename | null
  isClearingAll: boolean
  dataFiles: ProblemDataFilename[]
  dataTree: ProblemDataTreeNode[]
  errorMessage: string
  successMessage: string
}

export type ProblemDataPageAction =
  | { type: 'ready_save_started' }
  | { type: 'ready_save_succeeded'; message: string }
  | { type: 'ready_save_failed'; message: string }
  | { type: 'selected_file_set'; file: File | null }
  | { type: 'load_started' }
  | { type: 'load_succeeded'; files: ProblemDataFilename[]; tree: ProblemDataTreeNode[] }
  | { type: 'load_failed'; message: string }
  | { type: 'upload_started' }
  | { type: 'upload_succeeded'; message: string }
  | { type: 'upload_failed'; message: string }
  | { type: 'delete_started'; filename: ProblemDataFilename }
  | { type: 'delete_succeeded'; message: string }
  | { type: 'delete_failed'; message: string }
  | { type: 'clear_started' }
  | { type: 'clear_succeeded'; message: string }
  | { type: 'clear_failed'; message: string }
  | { type: 'error_cleared' }
  | { type: 'success_cleared' }

export const initialProblemDataPageState: ProblemDataPageState = {
  isSavingReady: false,
  selectedFile: null,
  isUploading: false,
  isLoadingFiles: true,
  deletingFilename: null,
  isClearingAll: false,
  dataFiles: [],
  dataTree: [],
  errorMessage: '',
  successMessage: '',
}

export function reduceProblemDataPageState(
  state: ProblemDataPageState,
  action: ProblemDataPageAction,
): ProblemDataPageState {
  switch (action.type) {
    case 'ready_save_started':
      return { ...state, isSavingReady: true, errorMessage: '', successMessage: '' }
    case 'ready_save_succeeded':
      return { ...state, isSavingReady: false, errorMessage: '', successMessage: action.message }
    case 'ready_save_failed':
      return { ...state, isSavingReady: false, errorMessage: action.message, successMessage: '' }
    case 'selected_file_set':
      return { ...state, selectedFile: action.file }
    case 'load_started':
      return { ...state, isLoadingFiles: true }
    case 'load_succeeded':
      return { ...state, isLoadingFiles: false, dataFiles: action.files, dataTree: action.tree }
    case 'load_failed':
      return { ...state, isLoadingFiles: false, errorMessage: action.message, successMessage: '' }
    case 'upload_started':
      return { ...state, isUploading: true, errorMessage: '', successMessage: '' }
    case 'upload_succeeded':
      return {
        ...state,
        selectedFile: null,
        isUploading: false,
        errorMessage: '',
        successMessage: action.message,
      }
    case 'upload_failed':
      return { ...state, isUploading: false, errorMessage: action.message, successMessage: '' }
    case 'delete_started':
      return { ...state, deletingFilename: action.filename, errorMessage: '', successMessage: '' }
    case 'delete_succeeded':
      return { ...state, deletingFilename: null, errorMessage: '', successMessage: action.message }
    case 'delete_failed':
      return { ...state, deletingFilename: null, errorMessage: action.message, successMessage: '' }
    case 'clear_started':
      return { ...state, isClearingAll: true, errorMessage: '', successMessage: '' }
    case 'clear_succeeded':
      return { ...state, isClearingAll: false, errorMessage: '', successMessage: action.message }
    case 'clear_failed':
      return { ...state, isClearingAll: false, errorMessage: action.message, successMessage: '' }
    case 'error_cleared':
      return { ...state, errorMessage: '' }
    case 'success_cleared':
      return { ...state, successMessage: '' }
  }
}
