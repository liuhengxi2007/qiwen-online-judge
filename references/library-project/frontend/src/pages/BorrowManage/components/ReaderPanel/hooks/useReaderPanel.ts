import { useMemo } from 'react'

import type { ReaderPanelState } from '../../../objects/BorrowManagePageState'
import type { ReaderEligibility } from '../objects/ReaderEligibility'

export function useReaderPanel(reader: ReaderPanelState): ReaderEligibility {
  return useMemo(() => {
    if (!reader.readerName.trim()) {
      return {
        tone: 'blocked',
        title: '等待读者信息',
        description: '输入借阅人姓名后，页面会根据当前借阅记录计算读者状态。',
      }
    }

    if (!reader.canBorrow) {
      return {
        tone: 'blocked',
        title: '暂不可借',
        description: '该读者存在逾期记录，需要先处理逾期后再借书。',
      }
    }

    return {
      tone: 'ready',
      title: '可以办理',
      description: '当前读者没有逾期阻断，可以继续选择图书。',
    }
  }, [reader.canBorrow, reader.readerName])
}
