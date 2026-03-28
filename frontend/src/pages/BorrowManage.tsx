import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowLeftRight, BookCopy, CalendarClock, FileSearch } from 'lucide-react'

import { FloatingPageTools } from '@/components/FloatingPageTools'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  BorrowRecordEntity,
  BorrowRecordStatuses,
  CirculationReviewStatuses,
  type BorrowCreationRequest,
  type BorrowCreationResponse,
  type ReturnProcessRequest,
  type ReturnProcessResponse,
} from '@/domain-types'
import { useMockSystem } from '@/hooks/useMockSystem'
import { cn } from '@/lib/utils'
import type { MockResultOption, PageEventDefinition } from '@/lib/mock-types'

const pageName = '借书管理页'
const route = '/library/admin/borrow/manage'

type CreateBorrowOption = MockResultOption<
  'borrow-create-success' | 'borrow-book-unavailable' | 'borrow-reader-restricted',
  BorrowCreationResponse
>
type ReturnBorrowOption = MockResultOption<
  'return-success' | 'return-damage-review' | 'return-system-error',
  ReturnProcessResponse
>

const createBorrowOptions: CreateBorrowOption[] = [
  {
    id: 'borrow-create-success',
    title: '登记成功',
    description: '生成新的借书记录，并返回 BorrowCreationResponse 成功结果，将图书状态更新为借出。',
    badge: 'success',
    noticeMessage: '借书登记成功，记录已加入列表。',
    payload: {
      ok: true,
      record: new BorrowRecordEntity(
        'BR-2025-0322',
        '数据库系统概念',
        '赵晴',
        '2025-03-22',
        '2025-04-05',
        BorrowRecordStatuses.Borrowing,
      ),
    },
  },
  {
    id: 'borrow-book-unavailable',
    title: '图书不可借',
    description: '当前图书已借出或已下架，返回 BorrowCreationResponse 失败结果，无法完成本次借书。',
    badge: 'error',
    payload: {
      ok: false,
      reason: 'book_unavailable',
      message: '当前图书已借出或已下架，无法完成本次借书。',
    },
  },
  {
    id: 'borrow-reader-restricted',
    title: '读者借阅受限',
    description: '读者存在逾期未还记录，返回 BorrowCreationResponse 失败结果，当前不能继续借书。',
    badge: 'warning',
    payload: {
      ok: false,
      reason: 'reader_restricted',
      message: '读者存在逾期未还记录，当前不能继续借书。',
    },
  },
]

const returnOptions: ReturnBorrowOption[] = [
  {
    id: 'return-success',
    title: '归还成功',
    description: '借阅记录更新为已归还，返回 ReturnProcessResponse 成功结果并让图书重新入库。',
    badge: 'success',
    noticeMessage: '归还办理完成，图书状态已恢复可借。',
    payload: {
      ok: true,
      reviewStatus: CirculationReviewStatuses.None,
      recordId: 'pending-record',
      returnedDate: '2025-03-22',
    },
  },
  {
    id: 'return-damage-review',
    title: '转人工处理',
    description: '图书有破损或异常，返回 ReturnProcessResponse 失败结果，记录状态维持不变，等待管理员复核。',
    badge: 'warning',
    payload: {
      ok: false,
      reviewStatus: CirculationReviewStatuses.ManualReview,
      message: '该记录已转人工复核，可先进入记录详情查看。',
      recordId: 'pending-record',
    },
  },
  {
    id: 'return-system-error',
    title: '提交失败',
    description: '系统未能写入归还结果，返回 ReturnProcessResponse 失败结果，页面保留当前状态。',
    badge: 'error',
    payload: {
      ok: false,
      reviewStatus: CirculationReviewStatuses.RetryRequired,
      message: '归还提交失败，请稍后重新办理。',
      recordId: 'pending-record',
    },
  },
]

const initialRecords: BorrowRecordEntity[] = [
  new BorrowRecordEntity('BR-2025-0318', '深入理解计算机系统', '李晨', '2025-03-18', '2025-04-01', BorrowRecordStatuses.Borrowing),
  new BorrowRecordEntity('BR-2025-0312', '算法导论', '周宁', '2025-03-12', '2025-03-26', BorrowRecordStatuses.Overdue),
  new BorrowRecordEntity('BR-2025-0305', '设计模式', '王雪', '2025-03-05', '2025-03-19', BorrowRecordStatuses.Returned, '2025-03-18'),
]

export default function BorrowManage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { openMockDialog, showNotice } = useMockSystem()
  const [records, setRecords] = useState<BorrowRecordEntity[]>(initialRecords)

  const pendingBookName = searchParams.get('bookName')
  const pendingBookId = searchParams.get('bookId')

  const activeCount = useMemo(
    () => records.filter((record) => record.status !== BorrowRecordStatuses.Returned).length,
    [records],
  )

  const handleCreateBorrow = (option: CreateBorrowOption) => {
    const request: BorrowCreationRequest = {
      bookId: pendingBookId ?? undefined,
      bookName: pendingBookName ?? undefined,
    }
    void request

    if (option.payload?.ok) {
      const nextRecord = pendingBookName
        ? new BorrowRecordEntity(
            option.payload.record.id,
            pendingBookName,
            option.payload.record.readerName,
            option.payload.record.borrowDate,
            option.payload.record.dueDate,
            option.payload.record.status,
            option.payload.record.returnDate,
          )
        : option.payload.record
      setRecords((current) => [nextRecord, ...current])
    }
  }

  const handleReturnBook = (recordId: string, option: ReturnBorrowOption) => {
    const request: ReturnProcessRequest = { recordId }
    void request

    const response = option.payload
    if (!response || !response.ok) {
      return
    }

    setRecords((current) =>
      current.map((record) =>
        record.id === recordId
          ? new BorrowRecordEntity(
              record.id,
              record.bookName,
              record.readerName,
              record.borrowDate,
              record.dueDate,
              BorrowRecordStatuses.Returned,
              response.returnedDate,
            )
          : record,
      ),
    )
  }

  const pageEvents: PageEventDefinition[] = [
    {
      id: 'scanner-input',
      label: '扫描枪录入借书',
      description: '模拟管理员通过扫码设备快速录入借书请求。',
      dialog: {
        pageName,
        route,
        eventName: '扫描枪录入借书',
        interactionName: '设备录入借书',
        title: '选择扫码录入结果',
        description: '模拟扫码设备录入借书信息后的系统处理。',
        options: createBorrowOptions,
        onSelect: (option) => handleCreateBorrow(option as CreateBorrowOption),
      },
    },
    {
      id: 'overdue-reminder',
      label: '发送逾期提醒',
      description: '模拟系统批量提醒逾期未还的读者。',
      dialog: {
        pageName,
        route,
        eventName: '发送逾期提醒',
        interactionName: '批量提醒读者',
        title: '选择提醒发送结果',
        description: '模拟系统向逾期读者发送提醒通知后的反馈。',
        options: [
          {
            id: 'reminder-success',
            title: '提醒发送成功',
            description: '系统已向逾期读者发送通知。',
            badge: 'success',
            noticeMessage: '逾期提醒已发送。',
          },
          {
            id: 'reminder-partial-failed',
            title: '部分发送失败',
            description: '部分读者联系方式异常，需要后续人工处理。',
            badge: 'warning',
          },
        ],
        onSelect: () => undefined,
      },
    },
    {
      id: 'refresh-records',
      label: '刷新借阅记录',
      description: '模拟页面重新拉取借书管理列表。',
      dialog: {
        pageName,
        route,
        eventName: '刷新借阅记录',
        interactionName: '刷新页面数据',
        title: '选择刷新结果',
        description: '模拟借阅记录刷新后的页面反馈。',
        options: [
          {
            id: 'refresh-success',
            title: '刷新成功',
            description: '列表数据更新成功，页面维持当前布局。',
            badge: 'success',
            noticeMessage: '借阅记录已刷新。',
          },
          {
            id: 'refresh-empty',
            title: '暂无新增变化',
            description: '未获取到新的借阅变更，保留当前数据。',
            badge: 'info',
          },
          {
            id: 'refresh-failed',
            title: '刷新失败',
            description: '网络异常，暂时无法更新借阅记录。',
            badge: 'error',
          },
        ],
        onSelect: () => undefined,
      },
    },
  ]

  return (
    <main className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,rgba(185,208,255,0.36),transparent_36%),linear-gradient(180deg,#f5f7fb_0%,#eef2f7_48%,#e8edf4_100%)]">
      <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.56),transparent_42%,rgba(15,23,42,0.04)_100%)]" />
      <div className="absolute inset-x-0 top-0 h-72 bg-[radial-gradient(circle_at_center,rgba(15,23,42,0.12),transparent_70%)] opacity-70" />

      <FloatingPageTools
        events={pageEvents}
        onEventSelect={(event) => openMockDialog(event.dialog)}
      />

      <section className="relative mx-auto flex min-h-screen w-full max-w-7xl items-center justify-center px-6 py-16 sm:px-8 lg:px-12">
        <div className="grid w-full items-start gap-8 lg:grid-cols-[0.86fr_1.14fr]">
          <div className="hidden lg:block">
            <div className="max-w-md space-y-4 pt-6">
              <div className="inline-flex items-center gap-3 rounded-full border border-white/70 bg-white/75 px-4 py-2 text-sm text-slate-600 shadow-[0_12px_30px_rgba(15,23,42,0.08)] backdrop-blur">
                <ArrowLeftRight className="size-4 text-slate-700" />
                借阅流转管理
              </div>
              <div className="space-y-4">
                <h1 className="text-4xl font-semibold tracking-tight text-slate-900">
                  图书馆管理系统 - 借书管理
                </h1>
                <p className="text-base leading-7 text-slate-600">
                  统一处理借书登记与归还入库，并支持进入单条借阅记录详情继续核对。
                </p>
              </div>
              <div className="rounded-[28px] border border-white/70 bg-white/70 p-5 shadow-[0_18px_48px_rgba(15,23,42,0.08)] backdrop-blur">
                <div className="flex items-center gap-3 text-slate-700">
                  <CalendarClock className="size-5" />
                  <span className="text-sm">当前待处理借阅记录 {activeCount} 条</span>
                </div>
              </div>
            </div>
          </div>

          <Card className="border-white/70 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-4 border-b border-slate-200/70 px-7 py-7 sm:px-8">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="space-y-2">
                  <div className="flex items-center gap-3 lg:hidden">
                    <div className="flex size-11 items-center justify-center rounded-2xl bg-slate-900 text-white shadow-sm">
                      <BookCopy className="size-5" />
                    </div>
                    <CardTitle className="text-xl text-slate-900">图书馆管理系统 - 借书管理</CardTitle>
                  </div>
                  <div className="hidden lg:block">
                    <CardTitle className="text-2xl text-slate-900">借书管理</CardTitle>
                  </div>
                  <CardDescription className="text-sm text-slate-500">
                    查看当前借阅记录并处理借书、还书
                  </CardDescription>
                </div>

                <Button
                  type="button"
                  className="h-11 rounded-xl bg-slate-900 px-5 text-sm font-medium text-white hover:bg-slate-800"
                  onClick={() =>
                    openMockDialog({
                      pageName,
                      route,
                      componentName: '新增借书按钮',
                      interactionName: '登记借书',
                      title: '选择借书登记结果',
                      description: pendingBookName
                        ? `模拟管理员为《${pendingBookName}》登记借书后的处理结果。`
                        : '模拟管理员提交新的借书请求后的处理结果。',
                      prioritySuggestion: '优先确认借书成功、图书不可借和读者受限三种状态。',
                      options: createBorrowOptions,
                      onSelect: handleCreateBorrow,
                    })
                  }
                >
                  登记借书
                </Button>
              </div>
            </CardHeader>

            <CardContent className="space-y-5 px-7 py-7 sm:px-8">
              {pendingBookName ? (
                <Alert className="rounded-2xl border-sky-200 bg-sky-50/90">
                  <AlertDescription className="text-sm text-sky-700">
                    已从图书列表页带入《{pendingBookName}》{pendingBookId ? `（${pendingBookId}）` : ''}，可直接登记借书并生成记录。
                  </AlertDescription>
                </Alert>
              ) : null}

              <div className="overflow-hidden rounded-3xl border border-slate-200/80 bg-white/90">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-slate-50/80 hover:bg-slate-50/80">
                      <TableHead className="h-12 pl-5 text-slate-500">借阅编号</TableHead>
                      <TableHead className="text-slate-500">图书</TableHead>
                      <TableHead className="text-slate-500">借阅人</TableHead>
                      <TableHead className="text-slate-500">借出日期</TableHead>
                      <TableHead className="text-slate-500">应还日期</TableHead>
                      <TableHead className="text-slate-500">状态</TableHead>
                      <TableHead className="pr-5 text-right text-slate-500">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {records.map((record) => (
                      <TableRow key={record.id} className="border-slate-200/80">
                        <TableCell className="pl-5 align-middle text-sm font-medium text-slate-700">
                          {record.id}
                        </TableCell>
                        <TableCell className="align-middle text-sm text-slate-700">
                          <div className="space-y-1">
                            <div>{record.bookName}</div>
                            {record.returnDate ? (
                              <div className="text-xs text-slate-400">归还日期 {record.returnDate}</div>
                            ) : null}
                          </div>
                        </TableCell>
                        <TableCell className="align-middle text-sm text-slate-700">
                          {record.readerName}
                        </TableCell>
                        <TableCell className="align-middle text-sm text-slate-600">
                          {record.borrowDate}
                        </TableCell>
                        <TableCell className="align-middle text-sm text-slate-600">
                          {record.dueDate}
                        </TableCell>
                        <TableCell className="align-middle">
                          <Badge
                            variant="outline"
                            className={cn(
                              'rounded-full border px-3 py-1 text-xs font-medium',
                              record.status === BorrowRecordStatuses.Borrowing &&
                                'border-sky-200 bg-sky-50 text-sky-700',
                              record.status === BorrowRecordStatuses.Overdue &&
                                'border-amber-200 bg-amber-50 text-amber-700',
                              record.status === BorrowRecordStatuses.Returned &&
                                'border-emerald-200 bg-emerald-50 text-emerald-700',
                            )}
                          >
                            {record.status}
                          </Badge>
                        </TableCell>
                        <TableCell className="pr-5 text-right align-middle">
                          <div className="flex justify-end gap-2">
                            <Button
                              type="button"
                              variant="ghost"
                              className="h-9 rounded-xl px-3 text-slate-600"
                              onClick={() => navigate(`/library/admin/borrow/record/${record.id}`)}
                            >
                              <FileSearch className="mr-2 size-4" />
                              详情
                            </Button>
                            {record.status === BorrowRecordStatuses.Returned ? (
                              <Button
                                type="button"
                                variant="ghost"
                                className="h-9 rounded-xl px-3 text-slate-500"
                                onClick={() => navigate(`/library/admin/borrow/record/${record.id}`)}
                              >
                                查看完成
                              </Button>
                            ) : (
                              <Button
                                type="button"
                                variant="outline"
                                className="h-9 rounded-xl border-slate-200 px-3 text-slate-700"
                                onClick={() =>
                                  openMockDialog({
                                    pageName,
                                    route,
                                    componentName: `${record.id} 归还按钮`,
                                    interactionName: '办理归还',
                                    title: `选择 ${record.bookName} 的归还处理结果`,
                                    description: '模拟管理员提交归还操作后的不同处理结果。',
                                    prioritySuggestion: '优先确认成功归还后的状态更新，以及异常归还时的处理分支。',
                                    options: returnOptions,
                                    onSelect: (option) => {
                                      if (option.payload?.reviewStatus === CirculationReviewStatuses.ManualReview) {
                                        showNotice(option.payload.message, 'info')
                                        return
                                      }

                                      if (option.payload?.reviewStatus === CirculationReviewStatuses.RetryRequired) {
                                        showNotice(option.payload.message, 'error')
                                        return
                                      }

                                      handleReturnBook(record.id, option)
                                    },
                                  })
                                }
                              >
                                办理归还
                              </Button>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}
