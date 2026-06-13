import { useEffect } from 'react'

/**
 * 在存在未保存内容时注册浏览器离开确认；enabled 为 false 时不产生事件监听副作用。
 */
export function useBeforeUnloadPrompt(enabled: boolean) {
  useEffect(() => {
    if (!enabled) {
      return
    }

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload)
    }
  }, [enabled])
}
