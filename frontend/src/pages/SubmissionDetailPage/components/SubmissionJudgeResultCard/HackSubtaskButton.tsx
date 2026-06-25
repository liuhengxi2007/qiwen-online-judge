import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { submissionIdValue } from '@/objects/submission/SubmissionId'

type HackSubtaskButtonProps = {
  submissionId: SubmissionId
  subtaskIndex: number
  label: string
  className?: string
}

/**
 * 子任务 hack 按钮，依据提交 id 和子任务索引生成 hack 路由。
 */
export function HackSubtaskButton({ submissionId, subtaskIndex, label, className }: HackSubtaskButtonProps) {
  return (
    <Button asChild size="sm" variant="outline" className={className}>
      <Link to={`/submissions/${submissionIdValue(submissionId)}/hack/${subtaskIndex}`}>{label}</Link>
    </Button>
  )
}
