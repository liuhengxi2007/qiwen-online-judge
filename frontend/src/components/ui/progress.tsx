import * as ProgressPrimitive from '@radix-ui/react-progress'
import type { ComponentProps } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 进度条组件，根据 value 百分比移动指示器，缺失 value 时按 0% 展示。
 */
function Progress({ className, value, ...props }: ComponentProps<typeof ProgressPrimitive.Root>) {
  return (
    <ProgressPrimitive.Root
      data-slot="progress"
      className={cn('bg-primary/20 relative h-2 w-full overflow-hidden rounded-full', className)}
      {...props}
    >
      <ProgressPrimitive.Indicator
        data-slot="progress-indicator"
        className="bg-primary h-full w-full flex-1 transition-all"
        style={{ transform: `translateX(-${100 - (value ?? 0)}%)` }}
      />
    </ProgressPrimitive.Root>
  )
}

export { Progress }
