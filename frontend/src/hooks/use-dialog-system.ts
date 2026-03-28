import { useMockSystem } from '@/hooks/useMockSystem'

export function useDialogSystem() {
  const { openMockDialog, openFeedbackDialog, showNotice } = useMockSystem()

  return {
    openDialog: openMockDialog,
    openFeedbackDialog,
    showNotice,
  }
}
