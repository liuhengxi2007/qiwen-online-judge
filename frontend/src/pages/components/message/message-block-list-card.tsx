import { Ban, Search } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import type { MessageBlockEntry } from '@/objects/message/response/MessageBlockEntry'
import { usernameValue } from '@/objects/user/user-parsers'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import { useI18n } from '@/system/i18n/use-i18n'

type MessageBlockListCardProps = {
  blockErrorMessage: string
  blockedUsers: MessageBlockEntry[]
  blockSearch: string
  isUpdatingBlocks: boolean
  setBlockSearch: (value: string) => void
  visibleBlockSuggestions: UserIdentity[]
  addBlock: (username: Username) => Promise<void>
  removeBlock: (username: Username) => Promise<void>
}

export function MessageBlockListCard({
  blockErrorMessage,
  blockedUsers,
  blockSearch,
  isUpdatingBlocks,
  setBlockSearch,
  visibleBlockSuggestions,
  addBlock,
  removeBlock,
}: MessageBlockListCardProps) {
  const { t } = useI18n()

  return (
    <Card id="message-blocks" className="scroll-mt-28 border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-violet-100 text-violet-700">
            <Ban className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{t('messages.blockListTitle')}</CardTitle>
            <CardDescription>{t('messages.blockListDescription')}</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        {blockErrorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{blockErrorMessage}</AlertDescription>
          </Alert>
        ) : null}
        <div className="space-y-2">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-3 size-4 text-slate-400" />
            <Input
              className="rounded-2xl border-slate-300 bg-white pl-10"
              value={blockSearch}
              onChange={(event) => setBlockSearch(event.target.value)}
              placeholder={t('messages.blockSearchPlaceholder')}
            />
          </div>
          <div className="space-y-2">
            {visibleBlockSuggestions.map((suggestion) => (
              <button
                key={usernameValue(suggestion.username)}
                type="button"
                className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-left transition hover:border-rose-300 hover:bg-rose-50"
                disabled={isUpdatingBlocks}
                onClick={() => {
                  void addBlock(suggestion.username)
                }}
              >
                <div>
                  <p className="font-medium text-slate-950">{suggestion.displayName}</p>
                  <p className="text-sm text-slate-600">@{usernameValue(suggestion.username)}</p>
                </div>
                <span className="text-sm font-medium text-rose-700">{t('messages.blockAdd')}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-3">
          {blockedUsers.length === 0 ? <p className="text-sm text-slate-500">{t('messages.blockEmpty')}</p> : null}
          {blockedUsers.map((entry) => (
            <div
              key={usernameValue(entry.user.username)}
              className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3"
            >
              <div>
                <p className="font-medium text-slate-950">{entry.user.displayName}</p>
                <p className="text-sm text-slate-600">@{usernameValue(entry.user.username)}</p>
              </div>
              <Button
                type="button"
                variant="outline"
                className="rounded-2xl border-rose-300 bg-white text-rose-950"
                disabled={isUpdatingBlocks}
                onClick={() => {
                  void removeBlock(entry.user.username)
                }}
              >
                {t('messages.blockRemove')}
              </Button>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
