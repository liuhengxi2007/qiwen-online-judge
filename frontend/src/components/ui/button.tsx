import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'
import type { ComponentProps } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 按语义和尺寸集中定义按钮样式，供 Button 与分页等复合组件复用。
 */
const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-2xl text-sm font-medium transition-[color,box-shadow] outline-none disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*="size-"])]:size-4 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground shadow-sm hover:bg-primary/90',
        create:
          'bg-emerald-300 text-emerald-950 shadow-sm hover:bg-emerald-400 focus-visible:ring-emerald-500',
        success:
          'bg-emerald-300 text-emerald-950 shadow-sm hover:bg-emerald-400 focus-visible:ring-emerald-500',
        destructive:
          'bg-destructive text-white shadow-sm hover:bg-destructive/90',
        destructiveOutline:
          'border border-rose-300 bg-white text-rose-700 shadow-sm hover:bg-rose-50 hover:text-rose-800 focus-visible:ring-rose-500',
        outline:
          'border border-border bg-background shadow-sm hover:bg-accent hover:text-accent-foreground',
        secondary:
          'bg-secondary text-secondary-foreground shadow-sm hover:bg-secondary/80',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-9 px-4 py-2',
        sm: 'h-8 rounded-xl px-3 text-xs',
        lg: 'h-10 px-6',
        icon: 'size-9 p-0',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)

/**
 * Button 的输入属性，兼容原生 button、样式变体和 Radix Slot 透传模式。
 */
type ButtonProps = ComponentProps<'button'> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }

/**
 * 基础按钮组件；根据 asChild 决定渲染原生 button 或把样式透传给子元素。
 */
function Button({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: ButtonProps) {
  const Comp = asChild ? Slot : 'button'

  return (
    <Comp
      data-slot="button"
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
