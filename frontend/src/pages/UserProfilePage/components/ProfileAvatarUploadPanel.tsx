import { useState } from 'react'

import { UploadUserAvatar } from '@/apis/user/UploadUserAvatar'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { UserProfileResponse } from '@/objects/user/response/UserProfileResponse'
import type { Username } from '@/objects/user/Username'
import { toAuthSession } from '@/pages/stores/auth/AuthSession'
import { sendMultipartAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

type ProfileAvatarUploadPanelProps = {
  onProfileUpdated: (profile: UserProfileResponse) => void
  onSessionUpdated: (session: SessionResponse) => void
  profile: UserProfileResponse
  targetUsername: Username
}

export function ProfileAvatarUploadPanel({
  onProfileUpdated,
  onSessionUpdated,
  profile,
  targetUsername,
}: ProfileAvatarUploadPanelProps) {
  const { t } = useI18n()
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  async function uploadAvatar() {
    if (!selectedFile) {
      setErrorMessage(t('userProfile.avatarRequired'))
      setSuccessMessage('')
      return
    }

    if (!isAcceptedAvatarFile(selectedFile)) {
      setErrorMessage(t('userProfile.avatarRequired'))
      setSuccessMessage('')
      return
    }

    setIsUploading(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const api = new UploadUserAvatar(targetUsername, selectedFile)
      const updatedUser = await sendMultipartAPI(api, api.formData())
      onSessionUpdated(toAuthSession(updatedUser))
      onProfileUpdated({
        ...profile,
        avatarUrl: updatedUser.avatarUrl,
        displayName: updatedUser.displayName,
      })
      setSelectedFile(null)
      setSuccessMessage(t('userProfile.avatarUploadSuccess'))
    } catch (error) {
      setErrorMessage(isHttpClientError(error) ? error.message : t('userProfile.avatarUploadFailed'))
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <div className="rounded-3xl border border-violet-100 bg-violet-50 p-6">
      <p className="text-sm font-semibold text-violet-950">{t('userProfile.avatarTitle')}</p>
      <p className="mt-1 text-sm text-violet-700">{t('userProfile.avatarDescription')}</p>

      {errorMessage ? (
        <Alert variant="destructive" className="mt-4 rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">{errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      {successMessage ? (
        <Alert className="mt-4 rounded-2xl border-emerald-200 bg-emerald-50/95">
          <AlertDescription className="text-emerald-700">{successMessage}</AlertDescription>
        </Alert>
      ) : null}

      <input
        accept=".png,.jpg,.jpeg,image/png,image/jpeg"
        className="mt-4 block w-full rounded-2xl border border-violet-100 bg-white px-3 py-2 text-sm text-slate-700 file:mr-4 file:rounded-xl file:border-0 file:bg-violet-100 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-violet-800"
        disabled={isUploading}
        onChange={(event) => {
          setSelectedFile(event.target.files?.[0] ?? null)
          setErrorMessage('')
          setSuccessMessage('')
        }}
        type="file"
      />

      <Button
        className="mt-4 rounded-2xl bg-violet-300 text-violet-950 hover:bg-violet-400"
        disabled={isUploading}
        onClick={() => {
          void uploadAvatar()
        }}
        type="button"
      >
        {isUploading ? t('userProfile.avatarUploading') : t('userProfile.avatarUpload')}
      </Button>
    </div>
  )
}

function isAcceptedAvatarFile(file: File): boolean {
  const lowerName = file.name.toLowerCase()
  return (
    (file.type === 'image/png' && lowerName.endsWith('.png')) ||
    (file.type === 'image/jpeg' && (lowerName.endsWith('.jpg') || lowerName.endsWith('.jpeg')))
  )
}
