import { Link } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/use-i18n'

export function ForbiddenPage() {
  const { t } = useI18n()
  usePageTitle(t('forbidden.pageTitle'))

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-3xl">
        <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <CardHeader className="text-center">
            <div className="mx-auto flex size-14 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
              <ShieldAlert className="size-6" />
            </div>
            <CardTitle className="mt-4 text-2xl text-slate-950">{t('forbidden.heading')}</CardTitle>
            <CardDescription>{t('forbidden.description')}</CardDescription>
          </CardHeader>
          <CardContent className="flex justify-center">
            <Button asChild className="rounded-2xl bg-amber-300 text-amber-950 hover:bg-amber-400">
              <Link to="/">{t('forbidden.backToDashboard')}</Link>
            </Button>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
