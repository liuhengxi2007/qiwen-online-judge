import type { ReactNode } from 'react'

import { cn } from '@/components/ui/class-names'

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
