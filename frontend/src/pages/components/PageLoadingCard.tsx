import { cn } from '@/components/ui/class-names'
import { Card, CardContent } from '@/components/ui/card'

/**
 * 页面加载卡片属性，包含展示文案、色调和外部样式扩展。
 */
type PageLoadingCardProps = {
  message: string
  tone?: 'slate' | 'stone'
  className?: string
}

/**
 * 页面级加载/空状态卡片，用于在主体内容区域展示轻量状态信息。
 */
export function PageLoadingCard({ message, tone = 'slate', className }: PageLoadingCardProps) {
  const toneClasses =
    tone === 'stone'
      ? {
          border: 'border-stone-200',
          shadow: 'shadow-[0_24px_60px_rgba(28,25,23,0.08)]',
          text: 'text-stone-500',
        }
      : {
          border: 'border-slate-200',
          shadow: 'shadow-[0_24px_60px_rgba(15,23,42,0.08)]',
          text: 'text-slate-500',
        }

  return (
    <Card className={cn(toneClasses.border, 'bg-white', toneClasses.shadow, className)}>
      <CardContent className={cn('py-10 text-sm', toneClasses.text)}>{message}</CardContent>
    </Card>
  )
}
