import { cva, type VariantProps } from 'class-variance-authority'
import type { ComponentProps } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 整行点击项的状态样式，覆盖建议项、通知项等列表内操作入口。
 */
const rowActionVariants = cva(
  'flex w-full cursor-pointer text-left outline-none transition disabled:pointer-events-none disabled:opacity-50 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
  {
    variants: {
      variant: {
        default:
          'border border-slate-200 bg-slate-50 hover:border-slate-300 hover:bg-white',
        accent:
          'border border-slate-200 bg-slate-50 hover:border-cyan-300 hover:bg-cyan-50',
        destructive:
          'border border-slate-200 bg-slate-50 hover:border-rose-300 hover:bg-rose-50',
        warning:
          'border border-amber-200 bg-amber-50 hover:border-amber-300 hover:bg-amber-100/60',
      },
      size: {
        default: 'items-center justify-between gap-4 rounded-2xl px-4 py-3',
        compact: 'flex-col gap-0.5 rounded-xl px-3 py-2 text-sm',
        spacious: 'items-start justify-between gap-4 rounded-3xl px-5 py-4',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)

/**
 * 原生 button 语义的整行操作组件，统一键盘焦点、禁用和 hover 反馈。
 */
function RowAction({
  className,
  variant,
  size,
  type = 'button',
  ...props
}: ComponentProps<'button'> & VariantProps<typeof rowActionVariants>) {
  return (
    <button
      data-slot="row-action"
      type={type}
      className={cn(rowActionVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { RowAction, rowActionVariants }
