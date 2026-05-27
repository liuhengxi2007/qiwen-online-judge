import type { ComponentProps } from 'react'
import { ChevronLeft, ChevronRight, MoreHorizontal } from 'lucide-react'

import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/components/ui/class-names'
import { translateMessage } from '@/system/i18n/messages'

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

function PaginationContent({ className, ...props }: ComponentProps<'ul'>) {
  return <ul className={cn('flex flex-row items-center gap-1', className)} {...props} />
}

function PaginationItem(props: ComponentProps<'li'>) {
  return <li {...props} />
}

type PaginationLinkProps = ComponentProps<'a'> & {
  isActive?: boolean
}

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
