import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Separator } from '@/components/ui/separator'
import { useMockSystem } from '@/hooks/useMockSystem'
import type { MockDialogRequest, MockResultOption } from '@/lib/mock-types'

import { ModalShell } from './ModalShell'

interface MockInteractionModalProps {
  request: MockDialogRequest
  onClose: () => void
  onOptionSelect: (request: MockDialogRequest, option: MockResultOption) => void
}

export function MockInteractionModal({
  request,
  onClose,
  onOptionSelect,
}: MockInteractionModalProps) {
  const { openFeedbackDialog } = useMockSystem()

  return (
    <ModalShell onClose={onClose}>
      <DialogHeader className="gap-3">
        <DialogDescription>结果选择面板</DialogDescription>
        <DialogTitle>{request.title}</DialogTitle>
      </DialogHeader>
      <DialogDescription>{request.description}</DialogDescription>
      <p className="text-sm text-muted-foreground">请选择一个处理结果，预览页面状态变化。</p>
      <Separator />

      <div className="grid gap-3">
        {request.options.map((option) => (
          <Button
            key={option.id}
            type="button"
            variant="outline"
            className="h-auto w-full flex-col items-stretch gap-2 px-4 py-4 text-left whitespace-normal"
            onClick={() => onOptionSelect(request, option)}
          >
            <div className="flex flex-col items-start justify-between gap-3 sm:flex-row sm:items-center">
              <span className="block font-bold">{option.title}</span>
              <Badge className="min-w-[72px] uppercase" variant={resolveBadgeVariant(option.badge)}>
                {option.badge}
              </Badge>
            </div>
            <p className="text-sm text-muted-foreground">{option.description}</p>
          </Button>
        ))}
      </div>

      <DialogFooter className="flex-wrap gap-3 sm:justify-start">
        <Button
          type="button"
          variant="secondary"
          onClick={() =>
            openFeedbackDialog({
              pageName: request.pageName,
              route: request.route,
              componentName: request.componentName,
              eventName: request.eventName,
              interactionName: request.interactionName,
              title: `补充 ${request.interactionName} 的处理意见`,
              description:
                '如果你觉得这个交互还缺少某种结果、跳转页或提示文案，可以在下一步补充说明。',
              prioritySuggestion:
                request.prioritySuggestion ?? '优先补齐缺失的状态分支、页面跳转和关键提示文案。',
              selectedMockResult: '补充交互处理意见',
            })
          }
        >
          补充处理意见
        </Button>
        <Button
          type="button"
          variant="secondary"
          onClick={onClose}
        >
          关闭
        </Button>
      </DialogFooter>
    </ModalShell>
  )
}

function resolveBadgeVariant(badge: MockResultOption['badge']) {
  switch (badge) {
    case 'success':
      return 'secondary'
    case 'warning':
      return 'outline'
    case 'error':
      return 'destructive'
    default:
      return 'outline'
  }
}
