import { useEffect, useReducer } from 'react'

/**
 * 读取当前时间戳，供 useReducer 初始化和定时 tick 复用。
 */
function currentTimeMillis(): number {
  return new Date().getTime()
}

/**
 * 按固定间隔返回当前时间戳；挂载期间注册 interval，卸载时清理。
 */
export function useNow(updateIntervalMs = 30_000): number {
  const [now, tick] = useReducer(currentTimeMillis, undefined, currentTimeMillis)

  useEffect(() => {
    const intervalId = window.setInterval(tick, updateIntervalMs)
    return () => window.clearInterval(intervalId)
  }, [updateIntervalMs])

  return now
}
