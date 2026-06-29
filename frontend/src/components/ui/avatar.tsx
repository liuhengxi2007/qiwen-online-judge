import * as AvatarPrimitive from '@radix-ui/react-avatar'
import type { ComponentProps } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 头像根组件，提供固定比例、圆形裁切和溢出隐藏的布局边界。
 */
function Avatar({ className, ...props }: ComponentProps<typeof AvatarPrimitive.Root>) {
  return (
    <AvatarPrimitive.Root
      data-slot="avatar"
      className={cn('relative flex size-10 shrink-0 overflow-hidden rounded-full', className)}
      {...props}
    />
  )
}

/**
 * 头像图片组件，承接 Radix 图片加载语义并填满头像容器。
 */
function AvatarImage({ className, ...props }: ComponentProps<typeof AvatarPrimitive.Image>) {
  return <AvatarPrimitive.Image data-slot="avatar-image" className={cn('aspect-square size-full', className)} {...props} />
}

/**
 * 头像降级组件，在图片缺失或加载失败时展示调用方传入的占位内容。
 */
function AvatarFallback({
  className,
  ...props
}: ComponentProps<typeof AvatarPrimitive.Fallback>) {
  return (
    <AvatarPrimitive.Fallback
      data-slot="avatar-fallback"
      className={cn('flex size-full items-center justify-center rounded-full bg-muted', className)}
      {...props}
    />
  )
}

export { Avatar, AvatarFallback, AvatarImage }
