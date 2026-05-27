import { Cpu } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import type { useSiteManageModel } from '../hooks/use-site-manage-model'
import { DateTimeText } from '@/pages/components/date-time-text'
import { useI18n } from '@/system/i18n/use-i18n'

type SiteManageModel = ReturnType<typeof useSiteManageModel>

export function SiteManageJudgersCard({ model }: { model: SiteManageModel }) {
  const { t } = useI18n()

  return (
    <Card className="mt-6 border-stone-200 bg-white shadow-[0_24px_60px_rgba(28,25,23,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
            <Cpu className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-stone-950">{t('siteManage.judgersTitle')}</CardTitle>
            <CardDescription>{t('siteManage.judgersDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {model.judgerListError ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{model.judgerListError}</AlertDescription>
          </Alert>
        ) : null}
        {model.isLoadingJudgers ? (
          <p className="text-sm text-stone-500">{t('siteManage.loadingJudgers')}</p>
        ) : model.judgers.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-stone-300 bg-stone-50 px-6 py-10 text-center">
            <p className="text-base font-medium text-stone-900">{t('siteManage.emptyJudgersTitle')}</p>
            <p className="mt-2 text-sm leading-7 text-stone-600">{t('siteManage.emptyJudgersDescription')}</p>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('siteManage.judgerId')}</TableHead>
                <TableHead>{t('siteManage.prefix')}</TableHead>
                <TableHead>{t('siteManage.host')}</TableHead>
                <TableHead>{t('siteManage.processId')}</TableHead>
                <TableHead>{t('siteManage.languages')}</TableHead>
                <TableHead>{t('siteManage.registeredAt')}</TableHead>
                <TableHead>{t('siteManage.lastHeartbeat')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {model.judgers.map((judger) => (
                <TableRow key={judger.judgerId}>
                  <TableCell className="font-medium text-stone-900">{judger.judgerId}</TableCell>
                  <TableCell>{judger.requestedPrefix}</TableCell>
                  <TableCell>{judger.host}</TableCell>
                  <TableCell>{judger.processId ?? t('siteManage.notAvailable')}</TableCell>
                  <TableCell>{judger.supportedLanguages.join(', ') || t('siteManage.notAvailable')}</TableCell>
                  <TableCell><DateTimeText value={judger.registeredAt} /></TableCell>
                  <TableCell><DateTimeText value={judger.lastHeartbeatAt} /></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}
