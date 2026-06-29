import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * 合并条件 className 并用 tailwind-merge 消解冲突，供所有 UI 组件统一处理样式扩展。
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
