import { MessageSquareMore, Search } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import { usernameValue } from '@/objects/user/Username'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 私信收件人搜索卡片属性，包含当前用户、搜索文本、建议和选择回调。
 */
type MessageRecipientSearchCardProps = {
  currentUsername: Username
  onSearchQueryChange: (value: string) => void
  onSuggestionSelect: (username: Username) => void
  searchError: string
  searchQuery: string
  suggestions: UserIdentity[]
}

/**
 * 私信收件人搜索卡片，展示用户建议并跳转到选中的会话。
 */
export function MessageRecipientSearchCard({
  currentUsername,
  onSearchQueryChange,
  onSuggestionSelect,
  searchError,
  searchQuery,
  suggestions,
}: MessageRecipientSearchCardProps) {
  const { t } = useI18n()

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-cyan-100 text-cyan-700">
            <MessageSquareMore className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{t('messages.newConversationTitle')}</CardTitle>
            <CardDescription>{t('messages.newConversationDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {searchError ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{searchError}</AlertDescription>
          </Alert>
        ) : null}
        <div className="space-y-2">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-3 size-4 text-slate-400" />
            <Input
              className="rounded-2xl border-slate-300 bg-white pl-10"
              value={searchQuery}
              onChange={(event) => onSearchQueryChange(event.target.value)}
              placeholder={t('messages.searchPlaceholder')}
            />
          </div>
          <div className="space-y-2">
            {suggestions
              .filter((suggestion) => suggestion.username !== currentUsername)
              .map((suggestion) => (
                <button
                  key={usernameValue(suggestion.username)}
                  type="button"
                  className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-left transition hover:border-cyan-300 hover:bg-cyan-50"
                  onClick={() => onSuggestionSelect(suggestion.username)}
                >
                  <div>
                    <p className="font-medium text-slate-950">{suggestion.displayName}</p>
                    <p className="text-sm text-slate-600">@{usernameValue(suggestion.username)}</p>
                  </div>
                  <span className="text-sm font-medium text-cyan-700">{t('messages.openConversation')}</span>
                </button>
              ))}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
