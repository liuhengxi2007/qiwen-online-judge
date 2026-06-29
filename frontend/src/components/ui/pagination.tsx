import type { ComponentProps } from 'react'
import { ChevronLeft, ChevronRight, MoreHorizontal } from 'lucide-react'

import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/components/ui/class-names'
import { translateMessage } from '@/system/i18n/messages'

/**
 * 分页导航根组件，提供 navigation 语义和居中布局。
 */
function Pagination({ className, ...props }: ComponentProps<'nav'>) {
  return (
    <nav
      role="navigation"
      aria-label="pagination"
      className={cn('mx-auto flex w-full justify-center', className)}
      {...props}
    />
  )
}

/**
 * 分页列表容器，负责页码项的横向排列。
 */
function PaginationContent({ className, ...props }: ComponentProps<'ul'>) {
  return <ul className={cn('flex flex-row items-center gap-1', className)} {...props} />
}

/**
 * 分页列表项组件，保留 li 语义供链接和省略号组合使用。
 */
function PaginationItem(props: ComponentProps<'li'>) {
  return <li {...props} />
}

/**
 * 分页链接属性，扩展当前页状态以同步 aria-current 和视觉样式。
 */
type PaginationLinkProps = ComponentProps<'a'> & {
  isActive?: boolean
}

/**
 * 页码链接组件，根据当前页状态选择按钮变体并透传锚点属性。
 */
function PaginationLink({ className, isActive, ...props }: PaginationLinkProps) {
  return (
    <a
      aria-current={isActive ? 'page' : undefined}
      className={cn(
        buttonVariants({
          variant: isActive ? 'outline' : 'ghost',
          size: 'icon',
        }),
        className,
      )}
      {...props}
    />
  )
}

/**
 * 上一页链接组件，内置本地化 aria-label 和方向图标。
 */
function PaginationPrevious({ className, ...props }: ComponentProps<typeof PaginationLink>) {
  return (
    <PaginationLink
      aria-label={translateMessage('common.pagination.previousAria')}
      className={cn('gap-1 px-2.5 sm:pl-2.5', className)}
      {...props}
    >
      <ChevronLeft className="size-4" />
      <span>{translateMessage('common.pagination.previous')}</span>
    </PaginationLink>
  )
}

/**
 * 下一页链接组件，内置本地化 aria-label 和方向图标。
 */
function PaginationNext({ className, ...props }: ComponentProps<typeof PaginationLink>) {
  return (
    <PaginationLink
      aria-label={translateMessage('common.pagination.nextAria')}
      className={cn('gap-1 px-2.5 sm:pr-2.5', className)}
      {...props}
    >
      <span>{translateMessage('common.pagination.next')}</span>
      <ChevronRight className="size-4" />
    </PaginationLink>
  )
}

/**
 * 分页省略号组件，表示中间页码被折叠并提供屏幕阅读器文案。
 */
function PaginationEllipsis({ className, ...props }: ComponentProps<'span'>) {
  return (
    <span aria-hidden className={cn('flex size-9 items-center justify-center', className)} {...props}>
      <MoreHorizontal className="size-4" />
      <span className="sr-only">{translateMessage('common.pagination.more')}</span>
    </span>
  )
}

export {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
}
