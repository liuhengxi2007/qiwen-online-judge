import type { ComponentProps } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card } from '@/components/ui/card'
import { cn } from '@/components/ui/class-names'

/**
 * Hack 页面专用卡片，统一白底、边框和阴影样式。
 */
export function HackCard({ className, ...props }: ComponentProps<typeof Card>) {
  return <Card className={cn('border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]', className)} {...props} />
}

/**
 * Hack 页面错误提示组件，输入用户可见错误文案并以危险提示展示。
 */
export function HackErrorAlert({ message }: { message: string }) {
  return (
    <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
      <AlertDescription className="text-rose-700">{message}</AlertDescription>
    </Alert>
  )
}
