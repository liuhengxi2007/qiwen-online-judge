import { useEffect, useRef, useState } from 'react'

import { listAcceptedRanklist, listContributionRanklist } from '@/features/auth/api/auth-client'
import type { UserAcceptedRanklistResponse, UserRanklistResponse } from '@/features/auth/domain/auth'

type UseRanklistQueryArgs = {
  acceptedPage: number
  contributionPage: number
}

export function useRanklistQuery({ acceptedPage, contributionPage }: UseRanklistQueryArgs) {
  const [contributionRanklist, setContributionRanklist] = useState<UserRanklistResponse | null>(null)
  const [acceptedRanklist, setAcceptedRanklist] = useState<UserAcceptedRanklistResponse | null>(null)
  const [isLoadingContributionRanklist, setIsLoadingContributionRanklist] = useState(false)
  const [isLoadingAcceptedRanklist, setIsLoadingAcceptedRanklist] = useState(false)
  const [contributionRanklistLoadError, setContributionRanklistLoadError] = useState('')
  const [acceptedRanklistLoadError, setAcceptedRanklistLoadError] = useState('')
  const contributionRequestIdRef = useRef(0)
  const acceptedRequestIdRef = useRef(0)

  useEffect(() => {
    let isCancelled = false
    contributionRequestIdRef.current += 1
    const nextRequestId = contributionRequestIdRef.current
    setContributionRanklist(null)
    setIsLoadingContributionRanklist(true)
    setContributionRanklistLoadError('')

    void listContributionRanklist(contributionPage)
      .then((response) => {
        if (isCancelled || contributionRequestIdRef.current !== nextRequestId) {
          return
        }

        setContributionRanklist(response)
        setIsLoadingContributionRanklist(false)
      })
      .catch(() => {
        if (isCancelled || contributionRequestIdRef.current !== nextRequestId) {
          return
        }

        setContributionRanklist(null)
        setIsLoadingContributionRanklist(false)
        setContributionRanklistLoadError('Unable to load contribution ranklist.')
      })

    return () => {
      isCancelled = true
    }
  }, [contributionPage])

  useEffect(() => {
    let isCancelled = false
    acceptedRequestIdRef.current += 1
    const nextRequestId = acceptedRequestIdRef.current
    setAcceptedRanklist(null)
    setIsLoadingAcceptedRanklist(true)
    setAcceptedRanklistLoadError('')

    void listAcceptedRanklist(acceptedPage)
      .then((response) => {
        if (isCancelled || acceptedRequestIdRef.current !== nextRequestId) {
          return
        }

        setAcceptedRanklist(response)
        setIsLoadingAcceptedRanklist(false)
      })
      .catch(() => {
        if (isCancelled || acceptedRequestIdRef.current !== nextRequestId) {
          return
        }

        setAcceptedRanklist(null)
        setIsLoadingAcceptedRanklist(false)
        setAcceptedRanklistLoadError('Unable to load accepted problem ranklist.')
      })

    return () => {
      isCancelled = true
    }
  }, [acceptedPage])

  return {
    acceptedRanklist,
    acceptedRanklistLoadError,
    contributionRanklist,
    contributionRanklistLoadError,
    isLoadingAcceptedRanklist,
    isLoadingContributionRanklist,
  }
}
