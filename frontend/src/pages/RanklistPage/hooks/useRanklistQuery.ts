import { useEffect, useRef, useState } from 'react'

import { ListAcceptedRanklist } from '@/apis/user/ListAcceptedRanklist'
import { ListContributionRanklist } from '@/apis/user/ListContributionRanklist'
import { ListRatingRanklist } from '@/apis/rating/ListRatingRanklist'
import type { RatingRanklistItem } from '@/objects/rating/response/RatingRanklistItem'
import type { UserAcceptedRanklistItem } from '@/objects/user/response/UserAcceptedRanklistItem'
import type { UserContributionRanklistItem } from '@/objects/user/response/UserContributionRanklistItem'
import type { PageResponse } from '@/objects/shared/PageResponse'
import { sendAPI } from '@/system/api/api-message'
import { translateMessage } from '@/system/i18n/messages'

type UseRanklistQueryArgs = {
  acceptedPage: number
  contributionPage: number
  ratingPage: number
}

export function useRanklistQuery({ acceptedPage, contributionPage, ratingPage }: UseRanklistQueryArgs) {
  const [contributionState, setContributionState] = useState<{
    page: number | null
    response: PageResponse<UserContributionRanklistItem> | null
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
  const [ratingState, setRatingState] = useState<{
    page: number | null
    response: PageResponse<RatingRanklistItem> | null
    errorMessage: string
  }>({
    page: null,
    response: null,
    errorMessage: '',
  })
  const contributionRequestIdRef = useRef(0)
  const acceptedRequestIdRef = useRef(0)
  const ratingRequestIdRef = useRef(0)

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

  useEffect(() => {
    let isCancelled = false
    ratingRequestIdRef.current += 1
    const nextRequestId = ratingRequestIdRef.current

    void sendAPI(new ListRatingRanklist({ page: ratingPage, pageSize: 10 }))
      .then((response) => {
        if (isCancelled || ratingRequestIdRef.current !== nextRequestId) {
          return
        }

        setRatingState({
          page: ratingPage,
          response,
          errorMessage: '',
        })
      })
      .catch(() => {
        if (isCancelled || ratingRequestIdRef.current !== nextRequestId) {
          return
        }

        setRatingState({
          page: ratingPage,
          response: null,
          errorMessage: translateMessage('ranklist.ratingLoadFailed'),
        })
      })

    return () => {
      isCancelled = true
    }
  }, [ratingPage])

  return {
    acceptedRanklist: acceptedState.page === acceptedPage ? acceptedState.response : null,
    acceptedRanklistLoadError: acceptedState.page === acceptedPage ? acceptedState.errorMessage : '',
    contributionRanklist: contributionState.page === contributionPage ? contributionState.response : null,
    contributionRanklistLoadError: contributionState.page === contributionPage ? contributionState.errorMessage : '',
    ratingRanklist: ratingState.page === ratingPage ? ratingState.response : null,
    ratingRanklistLoadError: ratingState.page === ratingPage ? ratingState.errorMessage : '',
    isLoadingAcceptedRanklist: acceptedState.page !== acceptedPage,
    isLoadingContributionRanklist: contributionState.page !== contributionPage,
    isLoadingRatingRanklist: ratingState.page !== ratingPage,
  }
}
