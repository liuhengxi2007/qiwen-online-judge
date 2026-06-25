import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import { ConfirmActionDialog } from '@/pages/components/ConfirmActionDialog'
import { useI18n } from '@/system/i18n/use-i18n'

import { ContestProblemList } from './ContestProblemList'
import { ProblemAttachInput } from './ProblemAttachInput'
import type { ContestManagePageModel } from '../hooks/useContestManagePageModel'

type ContestManageProblemCardProps = {
  model: ContestManagePageModel
  contest: ContestDetail
}

/**
 * 保留题目管理卡片 props 解构：model 负责操作状态，contest 负责当前题目列表。
 */
export function ContestManageProblemCard({ model, contest }: ContestManageProblemCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('contest.manage.problems')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        {model.problemErrorMessage ? (
          <Alert variant="destructive">
            <AlertDescription>{model.problemErrorMessage}</AlertDescription>
          </Alert>
        ) : null}
        {model.problemSuccessMessage ? (
          <Alert variant="success">
            <AlertDescription>{model.problemSuccessMessage}</AlertDescription>
          </Alert>
        ) : null}

        <ProblemAttachInput
          input={model.problemSearchInput}
          isLoading={model.isLoadingProblemSuggestions}
          suggestions={model.problemSuggestions}
          isAttaching={model.isAttachingProblem}
          onInputChange={model.setProblemSearchInput}
          onFocusChange={model.setIsProblemSearchFocused}
          onSuggestionSelect={model.selectProblemSuggestion}
          onAttach={() => {
            void model.attachProblem()
          }}
        />
        <ConfirmActionDialog
          open={model.isAttachWarningOpen}
          onOpenChange={model.closeAttachProblemWarning}
          title={t('contest.manage.attachWarningTitle')}
          description={t('contest.manage.attachWarningDescription')}
          confirmLabel={t('contest.manage.attachWarningConfirm')}
          cancelLabel={t('contest.manage.attachWarningCancel')}
          onConfirm={() => {
            void model.confirmAttachProblemWarning()
          }}
        />

        <ContestProblemList contest={contest} model={model} />
      </CardContent>
    </Card>
  )
}
