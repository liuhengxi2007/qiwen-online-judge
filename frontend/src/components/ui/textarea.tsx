import type { ComponentProps } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 统一样式的多行文本输入组件，透传原生 textarea 属性并提供最小高度。
 */
function Textarea({ className, ...props }: ComponentProps<'textarea'>) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        'flex min-h-16 w-full rounded-2xl border border-input bg-transparent px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 md:text-sm',
        className,
      )}
      {...props}
    />
  )
}

export { Textarea }
