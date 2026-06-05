import { useEffect, useReducer } from 'react'

function currentTimeMillis(): number {
  return new Date().getTime()
}

export function useNow(updateIntervalMs = 30_000): number {
  const [now, tick] = useReducer(currentTimeMillis, undefined, currentTimeMillis)

  useEffect(() => {
    const intervalId = window.setInterval(tick, updateIntervalMs)
    return () => window.clearInterval(intervalId)
  }, [updateIntervalMs])

  return now
}
