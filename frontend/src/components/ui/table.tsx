import type { HTMLAttributes, TableHTMLAttributes, TdHTMLAttributes, ThHTMLAttributes } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 表格根组件，外层提供横向滚动容器，内部保留原生 table 语义。
 */
function Table({ className, ...props }: TableHTMLAttributes<HTMLTableElement>) {
  return (
    <div className="relative w-full overflow-x-auto">
      <table className={cn('w-full caption-bottom text-sm', className)} {...props} />
    </div>
  )
}

/**
 * 表头组件，统一表头行的底部分隔样式。
 */
function TableHeader({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return <thead className={cn('[&_tr]:border-b', className)} {...props} />
}

/**
 * 表体组件，承载数据行并移除最后一行的多余边框。
 */
function TableBody({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return <tbody className={cn('[&_tr:last-child]:border-0', className)} {...props} />
}

/**
 * 表尾组件，用于汇总行或分页行，默认提供分隔和弱背景。
 */
function TableFooter({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <tfoot
      className={cn('border-t bg-muted/50 font-medium [&>tr]:last:border-b-0', className)}
      {...props}
    />
  )
}

/**
 * 表格行组件，提供行分隔和悬停背景反馈。
 */
function TableRow({ className, ...props }: HTMLAttributes<HTMLTableRowElement>) {
  return <tr className={cn('border-b transition-colors hover:bg-muted/50', className)} {...props} />
}

/**
 * 表头单元格组件，保留 th 语义并统一对齐、字号和弱文本颜色。
 */
function TableHead({ className, ...props }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cn('h-10 px-2 text-left align-middle font-medium text-muted-foreground', className)}
      {...props}
    />
  )
}

/**
 * 表格数据单元格组件，保留 td 语义并统一内边距和垂直对齐。
 */
function TableCell({ className, ...props }: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={cn('p-2 align-middle', className)} {...props} />
}

/**
 * 表格标题说明组件，适合展示数据来源或补充说明。
 */
function TableCaption({ className, ...props }: HTMLAttributes<HTMLTableCaptionElement>) {
  return <caption className={cn('mt-4 text-sm text-muted-foreground', className)} {...props} />
}

export {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
}
