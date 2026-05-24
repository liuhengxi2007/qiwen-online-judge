import { useEffect } from 'react'
import type { SetURLSearchParams } from 'react-router-dom'

import { getPageCorrection } from '@/shared/domain/pagination'

type UsePageSearchParamCorrectionArgs = {
  currentPage: number
  totalPages: number
  isLoading: boolean
  setSearchParams: SetURLSearchParams
}

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
