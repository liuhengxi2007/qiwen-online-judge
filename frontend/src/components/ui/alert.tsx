import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 警告提示的视觉变体集合，用于普通、成功、警告和危险反馈状态。
 */
const alertVariants = cva(
  'relative grid w-full gap-1 rounded-2xl border px-4 py-3 text-sm',
  {
    variants: {
      variant: {
        default: 'border-slate-200 bg-card text-card-foreground [&_[data-slot=alert-description]]:text-muted-foreground',
        success:
          'border-emerald-200 bg-emerald-50/95 text-emerald-900 [&_[data-slot=alert-description]]:text-emerald-700 [&_svg]:text-emerald-700',
        warning:
          'border-amber-200 bg-amber-50/95 text-amber-950 [&_[data-slot=alert-description]]:text-amber-900 [&_svg]:text-amber-700',
        destructive:
          'border-rose-200 bg-rose-50/95 text-rose-900 [&_[data-slot=alert-description]]:text-rose-700 [&_svg]:text-rose-700',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
)

/**
 * 警告提示根组件，提供 role="alert" 语义和统一边框背景样式。
 */
function Alert({
  className,
  variant,
  ...props
}: HTMLAttributes<HTMLDivElement> & VariantProps<typeof alertVariants>) {
  return <div role="alert" data-slot="alert" className={cn(alertVariants({ variant }), className)} {...props} />
}

/**
 * 警告提示标题组件，用于简短概括提示内容。
 */
function AlertTitle({ className, ...props }: HTMLAttributes<HTMLHeadingElement>) {
  return <h5 data-slot="alert-title" className={cn('font-medium leading-none tracking-tight', className)} {...props} />
}

/**
 * 警告提示正文组件，承载详细说明或富文本片段。
 */
function AlertDescription({ className, ...props }: HTMLAttributes<HTMLParagraphElement>) {
  return <div data-slot="alert-description" className={cn('text-sm text-muted-foreground [&_p]:leading-relaxed', className)} {...props} />
}

export { Alert, AlertDescription, AlertTitle }
