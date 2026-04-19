import { useEffect, useRef, useState } from 'react'

import { listAcceptedRanklist, listContributionRanklist } from '@/features/auth/api/auth-client'
import type { UserAcceptedRanklistResponse, UserRanklistResponse } from '@/features/auth/domain/auth'

type UseRanklistQueryArgs = {
  acceptedPage: number
  contributionPage: number
}

export function useRanklistQuery({ acceptedPage, contributionPage }: UseRanklistQueryArgs) {
  const [contributionState, setContributionState] = useState<{
    page: number | null
    response: UserRanklistResponse | null
    errorMessage: string
  }>({
    page: null,
    response: null,
    errorMessage: '',
  })
  const [acceptedState, setAcceptedState] = useState<{
    page: number | null
    response: UserAcceptedRanklistResponse | null
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

    void listContributionRanklist(contributionPage)
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
          errorMessage: 'Unable to load contribution ranklist.',
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

    void listAcceptedRanklist(acceptedPage)
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
          errorMessage: 'Unable to load accepted problem ranklist.',
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
