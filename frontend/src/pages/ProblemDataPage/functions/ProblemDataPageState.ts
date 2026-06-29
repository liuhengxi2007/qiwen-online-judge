import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDataTreeNode } from '@/objects/problem/response/ProblemDataTreeNode'

/**
 * 题目测试数据管理页状态，覆盖文件列表加载、上传/删除/清空、ready 保存和页面消息。
 */
export type ProblemDataPageState = {
  isSavingReady: boolean
  isRejudgingProblem: boolean
  selectedFile: File | null
  isUploading: boolean
  isLoadingFiles: boolean
  deletingPath: ProblemDataPath | null
  isClearingAll: boolean
  dataTree: ProblemDataTreeNode[]
  errorMessage: string
  successMessage: string
}

/**
 * 题目测试数据管理页 reducer 动作，描述各异步流程开始、成功、失败和消息清理事件。
 */
export type ProblemDataPageAction =
  | { type: 'ready_save_started' }
  | { type: 'ready_save_succeeded'; message: string }
  | { type: 'ready_save_failed'; message: string }
  | { type: 'problem_rejudge_started' }
  | { type: 'problem_rejudge_succeeded'; message: string }
  | { type: 'problem_rejudge_failed'; message: string }
  | { type: 'selected_file_set'; file: File | null }
  | { type: 'load_started' }
  | { type: 'load_succeeded'; tree: ProblemDataTreeNode[] }
  | { type: 'load_failed'; message: string }
  | { type: 'upload_started' }
  | { type: 'upload_succeeded'; message: string }
  | { type: 'upload_failed'; message: string }
  | { type: 'delete_started'; path: ProblemDataPath }
  | { type: 'delete_succeeded'; message: string }
  | { type: 'delete_failed'; message: string }
  | { type: 'clear_started' }
  | { type: 'clear_succeeded'; message: string }
  | { type: 'clear_failed'; message: string }
  | { type: 'error_cleared' }
  | { type: 'success_cleared' }

/**
 * 测试数据管理页初始状态；首次进入页面默认处于文件树加载中。
 */
export const initialProblemDataPageState: ProblemDataPageState = {
  isSavingReady: false,
  isRejudgingProblem: false,
  selectedFile: null,
  isUploading: false,
  isLoadingFiles: true,
  deletingPath: null,
  isClearingAll: false,
  dataTree: [],
  errorMessage: '',
  successMessage: '',
}

/**
 * 测试数据管理页 reducer，根据异步动作切换 loading 标记、当前路径和成功/错误消息。
 */
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
    case 'problem_rejudge_started':
      return { ...state, isRejudgingProblem: true, errorMessage: '', successMessage: '' }
    case 'problem_rejudge_succeeded':
      return { ...state, isRejudgingProblem: false, errorMessage: '', successMessage: action.message }
    case 'problem_rejudge_failed':
      return { ...state, isRejudgingProblem: false, errorMessage: action.message, successMessage: '' }
    case 'selected_file_set':
      return { ...state, selectedFile: action.file }
    case 'load_started':
      return { ...state, isLoadingFiles: true }
    case 'load_succeeded':
      return { ...state, isLoadingFiles: false, dataTree: action.tree }
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
      return { ...state, deletingPath: action.path, errorMessage: '', successMessage: '' }
    case 'delete_succeeded':
      return { ...state, deletingPath: null, errorMessage: '', successMessage: action.message }
    case 'delete_failed':
      return { ...state, deletingPath: null, errorMessage: action.message, successMessage: '' }
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
