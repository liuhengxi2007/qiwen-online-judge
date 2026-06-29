import type { HTMLAttributes } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 卡片根组件，提供边框、背景、阴影和纵向内容间距。
 */
function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      data-slot="card"
      className={cn('bg-card text-card-foreground flex flex-col gap-6 rounded-xl border py-6 shadow-sm', className)}
      {...props}
    />
  )
}

/**
 * 卡片头部组件，承载标题、说明和可选操作区域的网格布局。
 */
function CardHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      data-slot="card-header"
      className={cn('@container/card-header grid auto-rows-min grid-rows-[auto_auto] items-start gap-1.5 px-6 has-data-[slot=card-action]:grid-cols-[1fr_auto] [.border-b]:pb-6', className)}
      {...props}
    />
  )
}

/**
 * 卡片标题组件，用于卡片内主标题文本或标题行容器。
 */
function CardTitle({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      data-slot="card-title"
      className={cn('leading-none font-semibold', className)}
      {...props}
    />
  )
}

/**
 * 卡片说明组件；无子内容时不渲染，避免空白说明区域。
 */
function CardDescription({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  if (!props.children) {
    return null
  }

  return (
    <div
      data-slot="card-description"
      className={cn('text-sm text-muted-foreground', className)}
      {...props}
    />
  )
}

/**
 * 卡片主体组件，统一主体区域水平内边距。
 */
function CardContent({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div data-slot="card-content" className={cn('px-6', className)} {...props} />
}

/**
 * 卡片底部组件，承载操作按钮或补充信息并统一对齐。
 */
function CardFooter({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div data-slot="card-footer" className={cn('flex items-center px-6 [.border-t]:pt-6', className)} {...props} />
}

export { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle }
