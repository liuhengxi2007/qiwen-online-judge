import type { HTMLAttributes } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 骨架屏占位组件，用于数据加载期间占住内容区域并展示脉冲动画。
 */
function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('animate-pulse rounded-md bg-muted', className)} {...props} />
}

export { Skeleton }
