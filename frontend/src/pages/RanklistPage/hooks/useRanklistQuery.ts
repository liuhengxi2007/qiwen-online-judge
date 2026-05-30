import { useEffect, useRef, useState } from 'react'

import { ListAcceptedRanklist } from '@/apis/user/ListAcceptedRanklist'
import { ListContributionRanklist } from '@/apis/user/ListContributionRanklist'
import type { UserAcceptedRanklistItem } from '@/objects/user/response/UserAcceptedRanklistItem'
import type { UserRanklistItem } from '@/objects/user/response/UserRanklistItem'
import type { PageResponse } from '@/objects/shared/PageResponse'
import { sendAPI } from '@/system/api/api-message'
import { translateMessage } from '@/system/i18n/messages'

type UseRanklistQueryArgs = {
  acceptedPage: number
  contributionPage: number
}

export function useRanklistQuery({ acceptedPage, contributionPage }: UseRanklistQueryArgs) {
  const [contributionState, setContributionState] = useState<{
    page: number | null
    response: PageResponse<UserRanklistItem> | null
    errorMessage: string
  }>({
    page: null,
    response: null,
    errorMessage: '',
  })
  const [acceptedState, setAcceptedState] = useState<{
    page: number | null
    response: PageResponse<UserAcceptedRanklistItem> | null
    errorMessage: string
  }>({
    page: null,
    response: null,
    errorMessage: '',
  })
  const contributionRequestIdRef = useRef(0)
  const acceptedRequestIdRef = useRef(0)

  useEffect(() => {
    let isCancelled = false
    contributionRequestIdRef.current += 1
    const nextRequestId = contributionRequestIdRef.current

    void sendAPI(new ListContributionRanklist(contributionPage))
      .then((response) => {
        if (isCancelled || contributionRequestIdRef.current !== nextRequestId) {
          return
        }

        setContributionState({
          page: contributionPage,
          response,
          errorMessage: '',
        })
      })
      .catch(() => {
        if (isCancelled || contributionRequestIdRef.current !== nextRequestId) {
          return
        }

        setContributionState({
          page: contributionPage,
          response: null,
          errorMessage: translateMessage('ranklist.contributionLoadFailed'),
        })
      })

    return () => {
      isCancelled = true
    }
  }, [contributionPage])

  useEffect(() => {
    let isCancelled = false
    acceptedRequestIdRef.current += 1
    const nextRequestId = acceptedRequestIdRef.current

    void sendAPI(new ListAcceptedRanklist(acceptedPage))
      .then((response) => {
        if (isCancelled || acceptedRequestIdRef.current !== nextRequestId) {
          return
        }

        setAcceptedState({
          page: acceptedPage,
          response,
          errorMessage: '',
        })
      })
      .catch(() => {
        if (isCancelled || acceptedRequestIdRef.current !== nextRequestId) {
          return
        }

        setAcceptedState({
          page: acceptedPage,
          response: null,
          errorMessage: translateMessage('ranklist.acceptedLoadFailed'),
        })
      })

    return () => {
      isCancelled = true
    }
  }, [acceptedPage])

  return {
    acceptedRanklist: acceptedState.page === acceptedPage ? acceptedState.response : null,
    acceptedRanklistLoadError: acceptedState.page === acceptedPage ? acceptedState.errorMessage : '',
    contributionRanklist: contributionState.page === contributionPage ? contributionState.response : null,
    contributionRanklistLoadError: contributionState.page === contributionPage ? contributionState.errorMessage : '',
    isLoadingAcceptedRanklist: acceptedState.page !== acceptedPage,
    isLoadingContributionRanklist: contributionState.page !== contributionPage,
  }
}
