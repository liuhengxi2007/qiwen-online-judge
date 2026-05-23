import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { AppSectionBar } from '@/features/auth/components/app-section-bar'
import type { MessageConversationSummary } from '@/features/message/http/response/MessageConversationSummary'
import { useI18n } from '@/shared/i18n/use-i18n'

type MessageConversationPageHeaderProps = {
  conversation: MessageConversationSummary | null
}

export function MessageConversationPageHeader({ conversation }: MessageConversationPageHeaderProps) {
  const { t } = useI18n()

  return (
    <>
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-2">
          <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
          <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
            {conversation
              ? t('messages.conversationHeading', { displayName: conversation.otherUser.displayName })
              : t('messages.conversationFallbackHeading')}
          </h1>
          <p className="text-sm text-slate-600">{t('messages.conversationDescription')}</p>
        </div>
        <AncestorNavigation />
      </div>

      <AppSectionBar />
    </>
  )
}
