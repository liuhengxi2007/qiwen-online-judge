import type { ReactNode } from 'react'

import { cn } from '@/components/ui/class-names'

/**
 * 认证页外壳属性，调用方传入背景层、强调层和 hero 文案样式。
 */
type AuthPageShellProps = {
  children: ReactNode
  heroTitle: ReactNode
  heroDescription: ReactNode
  mainClassName: string
  overlayClassName: string
  firstAccentClassName: string
  secondAccentClassName: string
  gridClassName: string
  titleClassName: string
  descriptionClassName: string
}

/**
 * 登录/注册页外壳组件，提供全屏背景、hero 文案区和表单插槽。
 * 保留扁平 props 是为了让调用端直接声明各层样式插槽，避免再包一层样式对象降低 JSX 可读性。
 */
export function AuthPageShell({
  children,
  heroTitle,
  heroDescription,
  mainClassName,
  overlayClassName,
  firstAccentClassName,
  secondAccentClassName,
  gridClassName,
  titleClassName,
  descriptionClassName,
}: AuthPageShellProps) {
  return (
    <main className={cn('relative min-h-screen overflow-hidden', mainClassName)}>
      <div className={cn('absolute inset-0', overlayClassName)} />
      <div className={cn('absolute', firstAccentClassName)} />
      <div className={cn('absolute', secondAccentClassName)} />

      <section className="relative mx-auto flex min-h-screen max-w-6xl items-center px-6 py-14 sm:px-8 lg:px-12">
        <div className={cn('grid w-full items-center gap-10', gridClassName)}>
          <div className="space-y-6">
            <div className="max-w-xl space-y-4">
              <h1 className={cn('page-title-font text-4xl leading-tight font-semibold tracking-tight sm:text-5xl', titleClassName)}>
                {heroTitle}
              </h1>
              <p className={cn('text-base leading-8 sm:text-lg', descriptionClassName)}>{heroDescription}</p>
            </div>
          </div>

          {children}
        </div>
      </section>
    </main>
  )
}
