import { useState } from 'react'

import { DeleteUserAvatar } from '@/apis/user/DeleteUserAvatar'
import { UploadUserAvatar } from '@/apis/user/UploadUserAvatar'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { Username } from '@/objects/user/Username'
import { UserAvatar } from '@/pages/components/UserAvatar'
import { sendAPI, sendMultipartAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

type UserAvatarSettingsCardProps = {
  displayedUser: SessionResponse | null
  onUserUpdated: (user: SessionResponse) => void
  targetUsername: Username
}

export function UserAvatarSettingsCard({
  displayedUser,
  onUserUpdated,
  targetUsername,
}: UserAvatarSettingsCardProps) {
  const { t } = useI18n()
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  async function uploadAvatar() {
    if (!selectedFile) {
      setErrorMessage(t('userSettings.avatarRequired'))
      setSuccessMessage('')
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const api = new UploadUserAvatar(targetUsername, selectedFile)
      const updatedUser = await sendMultipartAPI(api, api.formData())
      onUserUpdated(updatedUser)
      setSelectedFile(null)
      setSuccessMessage(t('userSettings.avatarUploadSuccess'))
    } catch (error) {
      setErrorMessage(error instanceof HttpClientError ? error.message : t('userSettings.avatarUploadFailed'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function deleteAvatar() {
    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const updatedUser = await sendAPI(new DeleteUserAvatar(targetUsername))
      onUserUpdated(updatedUser)
      setSelectedFile(null)
      setSuccessMessage(t('userSettings.avatarDeleteSuccess'))
    } catch (error) {
      setErrorMessage(error instanceof HttpClientError ? error.message : t('userSettings.avatarDeleteFailed'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <CardTitle className="text-xl text-slate-950">{t('userSettings.avatarTitle')}</CardTitle>
        <CardDescription>{t('userSettings.avatarDescription')}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {displayedUser ? (
          <div className="flex items-center gap-4 rounded-2xl bg-slate-50 p-4">
            <UserAvatar
              avatarUrl={displayedUser.avatarUrl}
              className="size-16"
              displayName={displayedUser.displayName}
              fallbackClassName="text-lg"
            />
            <div className="min-w-0">
              <p className="text-sm font-medium text-slate-900">{t('userSettings.avatarPreview')}</p>
              <p className="mt-1 text-sm text-slate-500">{t('userSettings.avatarFileHelp')}</p>
            </div>
          </div>
        ) : null}

        {errorMessage ? (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {successMessage ? (
          <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/95">
            <AlertDescription className="text-emerald-700">{successMessage}</AlertDescription>
          </Alert>
        ) : null}

        <input
          accept="image/png,image/jpeg,image/webp"
          className="block w-full rounded-2xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 file:mr-4 file:rounded-xl file:border-0 file:bg-violet-100 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-violet-800"
          disabled={isSubmitting || !displayedUser}
          onChange={(event) => {
            setSelectedFile(event.target.files?.[0] ?? null)
            setErrorMessage('')
            setSuccessMessage('')
          }}
          type="file"
        />

        <div className="flex flex-wrap gap-3">
          <Button
            className="rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400"
            disabled={isSubmitting || !displayedUser}
            onClick={() => {
              void uploadAvatar()
            }}
            type="button"
          >
            {isSubmitting ? t('userSettings.avatarSaving') : t('userSettings.avatarUpload')}
          </Button>
          <Button
            className="rounded-2xl border-slate-300 bg-white text-slate-700"
            disabled={isSubmitting || !displayedUser || !displayedUser.avatarUrl}
            onClick={() => {
              void deleteAvatar()
            }}
            type="button"
            variant="outline"
          >
            {t('userSettings.avatarDelete')}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
