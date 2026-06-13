import { useEffect } from 'react'

/**
 * 同步浏览器文档标题；title 变化时更新 document.title。
 */
export function usePageTitle(title: string) {
  useEffect(() => {
    document.title = title
  }, [title])
}
