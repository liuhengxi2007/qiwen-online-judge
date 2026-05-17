import type { ReactNode } from 'react'

import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { useI18n } from '@/shared/i18n/use-i18n'

type UserAccountPageShellProps = {
  heading: string
  subheading: string
  children: ReactNode
}

export function UserAccountPageShell({
  heading,
  subheading,
  children,
}: UserAccountPageShellProps) {
  const { t } = useI18n()

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f7fafc_0%,#edf2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              {heading}
            </h1>
            <p className="text-sm text-slate-600">{subheading}</p>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

        <div className="grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">{children}</div>
      </section>
    </main>
  )
}
