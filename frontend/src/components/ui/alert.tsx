import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 警告提示的视觉变体集合，用于普通反馈和危险反馈两类状态。
 */
const alertVariants = cva(
  'relative w-full rounded-lg border px-4 py-3 text-sm grid gap-1',
  {
    variants: {
      variant: {
        default: 'bg-card text-card-foreground',
        destructive: 'border-destructive/50 text-destructive [&_svg]:text-destructive',
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
  return <div role="alert" className={cn(alertVariants({ variant }), className)} {...props} />
}

/**
 * 警告提示标题组件，用于简短概括提示内容。
 */
function AlertTitle({ className, ...props }: HTMLAttributes<HTMLHeadingElement>) {
  return <h5 className={cn('font-medium leading-none tracking-tight', className)} {...props} />
}

/**
 * 警告提示正文组件，承载详细说明或富文本片段。
 */
function AlertDescription({ className, ...props }: HTMLAttributes<HTMLParagraphElement>) {
  return <div className={cn('text-sm text-muted-foreground [&_p]:leading-relaxed', className)} {...props} />
}

export { Alert, AlertDescription, AlertTitle }
