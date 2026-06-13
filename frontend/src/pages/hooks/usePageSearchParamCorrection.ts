import { useEffect } from 'react'
import type { SetURLSearchParams } from 'react-router-dom'

import { getPageCorrection } from '@/pages/objects/Pagination'

/**
 * 分页 URL 修正 hook 的输入，包含当前分页状态和 react-router 的参数写入函数。
 */
type UsePageSearchParamCorrectionArgs = {
  currentPage: number
  totalPages: number
  isLoading: boolean
  setSearchParams: SetURLSearchParams
}

/**
 * 在列表加载完成后修正越界 page 参数，避免用户停留在已不存在的分页。
 */
export function usePageSearchParamCorrection({
  currentPage,
  totalPages,
  isLoading,
  setSearchParams,
}: UsePageSearchParamCorrectionArgs) {
  useEffect(() => {
    if (isLoading) {
      return
    }

    const correction = getPageCorrection(currentPage, totalPages)
    if (correction.kind === 'none') {
      return
    }

    setSearchParams((previousSearchParams) => {
      const nextSearchParams = new URLSearchParams(previousSearchParams)
      if (correction.kind === 'delete') {
        nextSearchParams.delete('page')
      } else {
        nextSearchParams.set('page', String(correction.page))
      }
      return nextSearchParams
    })
  }, [currentPage, isLoading, setSearchParams, totalPages])
}
