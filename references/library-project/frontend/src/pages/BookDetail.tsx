import { useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  ArrowLeft,
  BookCopy,
  BookOpenText,
  FileSearch,
  LibraryBig,
  MapPinned,
  NotebookText,
  PencilLine,
  ScanSearch,
  Trash2,
} from 'lucide-react'

import { FloatingPageTools } from '@/components/FloatingPageTools'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import {
  BookInventoryStatuses,
  CirculationReviewStatuses,
  type ReturnProcessResponse,
} from '@/domain-types'
import { useMockSystem } from '@/hooks/useMockSystem'
import type { MockResultOption, PageEventDefinition } from '@/lib/mock-types'

const pageName = '图书详情页'
const route = '/library/book/detail/:id'

type DetailBorrowOption = MockResultOption<'borrow-success' | 'borrow-unavailable' | 'borrow-limit'>
type DetailReturnOption = MockResultOption<
  'return-success' | 'return-overdue' | 'return-mismatch',
  ReturnProcessResponse
>

const borrowOptions: DetailBorrowOption[] = [
  {
    id: 'borrow-success',
    title: '借阅成功',
    description: '图书成功借出，页面切换为借出状态并生成可查看的借阅记录。',
    badge: 'success',
    noticeMessage: '借阅成功，图书状态已更新。',
  },
  {
    id: 'borrow-unavailable',
    title: '库存不足',
    description: '当前没有可借副本，保留在详情页并提示暂不可借。',
    badge: 'warning',
  },
  {
    id: 'borrow-limit',
    title: '借阅数量超限',
    description: '当前管理员代办借书时触发借阅上限校验，显示失败提示。',
    badge: 'error',
  },
]

const returnOptions: DetailReturnOption[] = [
  {
    id: 'return-success',
    title: '归还成功',
    description: '图书归还入库，页面切换为可借状态，并生成可查看的归还记录。',
    badge: 'success',
    noticeMessage: '归还成功，图书已恢复可借。',
    payload: {
      ok: true,
      reviewStatus: CirculationReviewStatuses.None,
      recordId: 'RT-2025-0416',
      returnedDate: '2025-04-16',
    },
  },
  {
    id: 'return-overdue',
    title: '归还成功并提示逾期',
    description: '图书已归还，同时提供继续进入还书管理页处理逾期后续。',
    badge: 'warning',
    noticeMessage: '图书已归还，存在逾期记录待处理。',
    payload: {
      ok: false,
      reviewStatus: CirculationReviewStatuses.FineRequired,
      message: '图书已归还，当前记录存在逾期，请继续进入还书管理页处理。',
      recordId: 'RT-2025-0416',
    },
  },
  {
    id: 'return-mismatch',
    title: '归还失败',
    description: '当前图书借阅记录异常，无法完成归还。',
    badge: 'error',
    payload: {
      ok: false,
      reviewStatus: CirculationReviewStatuses.RetryRequired,
      message: '借阅记录校验失败，当前无法执行归还。',
      recordId: 'RT-2025-0416',
    },
  },
]

const deleteOptions: MockResultOption[] = [
  {
    id: 'delete-success',
    title: '删除成功',
    description: '图书记录删除完成，返回图书列表页。',
    badge: 'success',
    noticeMessage: '图书已删除，正在返回列表。',
  },
  {
    id: 'delete-blocked',
    title: '存在借阅记录',
    description: '图书当前仍有关联借阅记录，不允许删除。',
    badge: 'error',
  },
]

export default function BookDetail() {
  const navigate = useNavigate()
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const { openMockDialog } = useMockSystem()

  const [status, setStatus] = useState<(typeof BookInventoryStatuses)[keyof typeof BookInventoryStatuses]>(
    searchParams.get('source') === 'add'
      ? BookInventoryStatuses.Available
      : BookInventoryStatuses.Available,
  )
  const [feedbackMessage, setFeedbackMessage] = useState('')
  const [lastRecordId, setLastRecordId] = useState('')
  const [lastAction, setLastAction] = useState<'borrow' | 'return' | 'overdue' | ''>('')

  const book = useMemo(
    () => ({
      id: id ?? 'BK-1024',
      title: id === 'BK-2001' ? '馆藏新编样书' : '百年孤独',
      author: id === 'BK-2001' ? '系统示例' : '加西亚·马尔克斯',
      isbn: id === 'BK-2001' ? '9787111888888' : '9787544291170',
      category: id === 'BK-2001' ? '管理' : '外国文学',
      publisher: id === 'BK-2001' ? '图书馆馆藏中心' : '南海出版公司',
      shelf: id === 'BK-2001' ? 'N-02-08' : 'A-03-14',
      totalStock: 4,
      availableStock: status === BookInventoryStatuses.Available ? 3 : 0,
      borrower: status === BookInventoryStatuses.Borrowed ? '李晨' : '',
      borrowDate: status === BookInventoryStatuses.Borrowed ? '2025-02-18' : '',
      dueDate: status === BookInventoryStatuses.Borrowed ? '2025-03-18' : '',
      summary:
        id === 'BK-2001'
          ? '这是新增图书后的确认样例，可继续返回列表定位新记录，或进入编辑页继续完善馆藏资料。'
          : '布恩迪亚家族七代人的命运在马孔多小镇交织展开，现实与奇想并行，构成一部兼具史诗感与私人命运的小说。',
    }),
    [id, status],
  )

  const handleBorrowResult = (option: MockResultOption) => {
    if (option.id === 'borrow-success') {
      setStatus(BookInventoryStatuses.Borrowed)
      setFeedbackMessage('')
      setLastRecordId('BR-2025-0416')
      setLastAction('borrow')
      return
    }

    if (option.id === 'borrow-unavailable') {
      setFeedbackMessage('当前无可借副本，请等待归还后再处理。')
      setLastAction('')
      return
    }

    setFeedbackMessage('该读者已达到借阅上限，暂时无法继续借书。')
    setLastAction('')
  }

  const handleReturnResult = (option: DetailReturnOption) => {
    if (option.payload?.ok) {
      setStatus(BookInventoryStatuses.Available)
      setFeedbackMessage('')
      setLastRecordId(option.payload.recordId)
      setLastAction('return')
      return
    }

    if (option.payload?.reviewStatus === CirculationReviewStatuses.FineRequired) {
      setStatus(BookInventoryStatuses.Available)
      setFeedbackMessage(option.payload.message)
      setLastRecordId(option.payload.recordId)
      setLastAction('overdue')
      return
    }

    setFeedbackMessage(option.payload?.message ?? '借阅记录校验失败，当前无法执行归还。')
    setLastAction('')
  }

  const handleDeleteResult = (option: MockResultOption) => {
    if (option.id === 'delete-success') {
      setFeedbackMessage('')
      navigate('/library/admin/book/list')
      return
    }

    setFeedbackMessage('该图书仍有关联借阅记录，暂时不能删除。')
  }

  const pageEvents: PageEventDefinition[] = [
    {
      id: 'refresh-detail',
      label: '刷新详情数据',
      description: '模拟管理员重新进入页面或刷新后，详情数据的加载结果。',
      dialog: {
        pageName,
        route,
        eventName: '刷新详情数据',
        interactionName: '页面数据重载',
        title: '选择详情数据加载结果',
        description: '模拟图书详情页重新加载时的不同页面状态。',
        options: [
          {
            id: 'refresh-success',
            title: '加载最新详情',
            description: '保持当前页面内容，并提示数据已刷新。',
            badge: 'success',
            noticeMessage: '图书详情已刷新为最新数据。',
          },
          {
            id: 'refresh-archived',
            title: '图书已不存在',
            description: '图书记录已被删除，返回图书列表页。',
            badge: 'warning',
          },
          {
            id: 'refresh-error',
            title: '加载失败',
            description: '详情接口异常，页面停留并显示错误提示。',
            badge: 'error',
          },
        ],
        onSelect: (option) => {
          if (option.id === 'refresh-archived') {
            navigate('/library/admin/book/list')
            return
          }

          if (option.id === 'refresh-error') {
            setFeedbackMessage('图书详情加载失败，请稍后重试。')
            return
          }

          setFeedbackMessage('')
        },
      },
    },
    {
      id: 'scan-book-code',
      label: '扫码定位图书',
      description: '模拟管理员扫描图书编码后定位到当前详情页的处理结果。',
      dialog: {
        pageName,
        route,
        eventName: '扫码定位图书',
        interactionName: '扫码进入详情',
        title: '选择扫码定位结果',
        description: '模拟管理员通过扫码枪或馆藏码进入图书详情页的不同反馈。',
        options: [
          {
            id: 'scan-success',
            title: '定位成功',
            description: '维持当前详情页内容，并提示已定位到该图书。',
            badge: 'success',
            noticeMessage: '已定位到图书详情。',
          },
          {
            id: 'scan-invalid',
            title: '编码无效',
            description: '编码无法识别，在页面内显示错误提示。',
            badge: 'error',
          },
        ],
        onSelect: (option) => {
          setFeedbackMessage(option.id === 'scan-invalid' ? '未识别到有效图书编码。' : '')
        },
      },
    },
  ]

  return (
    <main className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,rgba(185,208,255,0.34),transparent_34%),linear-gradient(180deg,#f5f7fb_0%,#eef2f7_48%,#e8edf4_100%)]">
      <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.56),transparent_42%,rgba(15,23,42,0.04)_100%)]" />
      <div className="absolute inset-x-0 top-0 h-72 bg-[radial-gradient(circle_at_center,rgba(15,23,42,0.12),transparent_70%)] opacity-70" />

      <FloatingPageTools
        events={pageEvents}
        onEventSelect={(event) => openMockDialog(event.dialog)}
      />

      <section className="relative mx-auto flex min-h-screen w-full max-w-6xl items-center justify-center px-6 py-16 sm:px-8 lg:px-12">
        <div className="grid w-full max-w-5xl gap-8 lg:grid-cols-[0.88fr_1.12fr]">
          <div className="hidden lg:block">
            <div className="max-w-md space-y-4">
              <div className="inline-flex items-center gap-3 rounded-full border border-white/70 bg-white/75 px-4 py-2 text-sm text-slate-600 shadow-[0_12px_30px_rgba(15,23,42,0.08)] backdrop-blur">
                <LibraryBig className="size-4 text-slate-700" />
                馆藏图书详情
              </div>
              <div className="space-y-4">
                <h1 className="text-4xl font-semibold tracking-tight text-slate-900">
                  图书馆管理系统 - 图书详情
                </h1>
                <p className="text-base leading-7 text-slate-600">
                  查看图书信息，并直接处理借书、还书、编辑与删除操作。
                </p>
              </div>
            </div>
          </div>

          <Card className="border-white/70 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-4 border-b border-slate-200/70 px-7 py-7 sm:px-8">
              <div className="flex items-start justify-between gap-4">
                <div className="space-y-3">
                  <div className="flex items-center gap-3 lg:hidden">
                    <div className="flex size-11 items-center justify-center rounded-2xl bg-slate-900 text-white shadow-sm">
                      <BookOpenText className="size-5" />
                    </div>
                    <CardTitle className="text-xl text-slate-900">图书馆管理系统 - 图书详情</CardTitle>
                  </div>
                  <div className="space-y-2">
                    <div className="flex items-center gap-3">
                      <CardTitle className="text-2xl text-slate-900">{book.title}</CardTitle>
                      <Badge
                        variant="outline"
                        className={
                          status === BookInventoryStatuses.Available
                            ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                            : 'border-amber-200 bg-amber-50 text-amber-700'
                        }
                      >
                        {status === BookInventoryStatuses.Available ? '可借' : '借出中'}
                      </Badge>
                    </div>
                    <CardDescription className="text-sm text-slate-500">
                      图书编号：{book.id}
                    </CardDescription>
                  </div>
                </div>

                <Button
                  type="button"
                  variant="outline"
                  className="rounded-xl border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                  onClick={() => navigate('/library/admin/book/list')}
                >
                  <ArrowLeft className="size-4" />
                  返回列表
                </Button>
              </div>
            </CardHeader>

            <CardContent className="space-y-6 px-7 py-7 sm:px-8">
              {searchParams.get('source') === 'add' ? (
                <Alert className="rounded-2xl border-sky-200 bg-sky-50/90">
                  <AlertDescription className="flex flex-wrap items-center gap-3 text-sm text-sky-700">
                    新图书已创建完成，你可以先核对详情，再返回列表定位这条新记录。
                    <Button
                      type="button"
                      variant="outline"
                      className="h-9 rounded-xl border-sky-200 bg-white text-sky-700 hover:bg-sky-100"
                      onClick={() => navigate(`/library/admin/book/list?highlight=${book.id}`)}
                    >
                      查看列表中的新记录
                    </Button>
                  </AlertDescription>
                </Alert>
              ) : null}

              {searchParams.get('updated') === '1' ? (
                <Alert className="rounded-2xl border-emerald-200 bg-emerald-50/90">
                  <AlertDescription className="text-sm text-emerald-700">
                    图书信息已更新，可继续核对详情或返回列表查看最新状态。
                  </AlertDescription>
                </Alert>
              ) : null}

              <div className="grid gap-4 sm:grid-cols-2">
                <DetailItem icon={BookCopy} label="作者" value={book.author} />
                <DetailItem icon={ScanSearch} label="ISBN" value={book.isbn} />
                <DetailItem icon={NotebookText} label="分类" value={book.category} />
                <DetailItem icon={MapPinned} label="馆藏位置" value={book.shelf} />
                <DetailItem icon={LibraryBig} label="出版社" value={book.publisher} />
                <DetailItem
                  icon={BookOpenText}
                  label="库存"
                  value={`${book.availableStock} / ${book.totalStock} 可借`}
                />
              </div>

              <Separator />

              <div className="space-y-3">
                <h2 className="text-sm font-medium text-slate-700">内容简介</h2>
                <div className="rounded-2xl border border-slate-200/80 bg-slate-50/80 px-4 py-4 text-sm leading-7 text-slate-600">
                  {book.summary}
                </div>
              </div>

              {status === BookInventoryStatuses.Borrowed ? (
                <div className="grid gap-4 rounded-3xl border border-amber-200/80 bg-amber-50/70 p-5 sm:grid-cols-3">
                  <DetailText label="借阅人" value={book.borrower} />
                  <DetailText label="借出日期" value={book.borrowDate} />
                  <DetailText label="应还日期" value={book.dueDate} />
                </div>
              ) : null}

              <div className="min-h-16">
                {feedbackMessage ? (
                  <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/90">
                    <AlertDescription className="text-sm text-rose-700">
                      {feedbackMessage}
                    </AlertDescription>
                  </Alert>
                ) : null}
              </div>

              {lastAction ? (
                <Alert className="rounded-2xl border-slate-200 bg-slate-50/90">
                  <AlertDescription className="flex flex-wrap items-center gap-3 text-sm text-slate-700">
                    {lastAction === 'borrow' ? '借书已完成，可立即查看新生成的借阅记录。' : null}
                    {lastAction === 'return' ? '还书已完成，可查看归还记录继续核对结果。' : null}
                    {lastAction === 'overdue' ? '逾期归还已登记，可继续查看记录或进入还书管理页处理后续。' : null}
                    <Button
                      type="button"
                      variant="outline"
                      className="h-9 rounded-xl"
                      onClick={() => navigate(`/library/admin/borrow/record/${lastRecordId}`)}
                    >
                      <FileSearch className="mr-2 size-4" />
                      查看记录
                    </Button>
                    {lastAction === 'overdue' ? (
                      <Button
                        type="button"
                        variant="outline"
                        className="h-9 rounded-xl"
                        onClick={() =>
                          navigate(`/library/admin/return/manage?bookId=${book.id}&bookName=${encodeURIComponent(book.title)}`)
                        }
                      >
                        前往还书管理
                      </Button>
                    ) : null}
                  </AlertDescription>
                </Alert>
              ) : null}

              <div className="flex flex-wrap gap-3">
                {status === BookInventoryStatuses.Available ? (
                  <Button
                    type="button"
                    className="h-11 rounded-xl bg-slate-900 px-5 text-white hover:bg-slate-800"
                    onClick={() =>
                      openMockDialog({
                        pageName,
                        route,
                        componentName: '借书按钮',
                        interactionName: '图书借阅',
                        title: '选择借书处理结果',
                        description: '模拟管理员在图书详情页发起借书操作后的不同结果。',
                        prioritySuggestion: '优先确认借阅成功后的状态切换，以及库存不足时的提示。',
                        options: borrowOptions,
                        onSelect: handleBorrowResult,
                      })
                    }
                  >
                    借书
                  </Button>
                ) : (
                  <Button
                    type="button"
                    className="h-11 rounded-xl bg-slate-900 px-5 text-white hover:bg-slate-800"
                    onClick={() =>
                      openMockDialog({
                        pageName,
                        route,
                        componentName: '还书按钮',
                        interactionName: '图书归还',
                        title: '选择还书处理结果',
                        description: '模拟管理员在图书详情页处理归还操作后的不同结果。',
                        prioritySuggestion: '优先确认归还成功后的状态恢复，以及借阅记录异常时的提示。',
                        options: returnOptions,
                        onSelect: handleReturnResult,
                      })
                    }
                  >
                    还书
                  </Button>
                )}

                <Button
                  type="button"
                  variant="outline"
                  className="h-11 rounded-xl border-slate-200 bg-white px-5 text-slate-700 hover:bg-slate-50"
                  onClick={() => navigate(`/library/admin/book/edit/${book.id}`)}
                >
                  <PencilLine className="size-4" />
                  编辑图书
                </Button>

                <Button
                  type="button"
                  variant="outline"
                  className="h-11 rounded-xl border-slate-200 bg-white px-5 text-slate-700 hover:bg-slate-50"
                  onClick={() =>
                    openMockDialog({
                      pageName,
                      route,
                      componentName: '删除图书按钮',
                      interactionName: '删除图书',
                      title: '选择删除图书结果',
                      description: '模拟管理员在详情页删除图书记录后的不同处理结果。',
                      prioritySuggestion: '优先确认删除受阻时的提示，以及删除成功后的返回路径。',
                      options: deleteOptions,
                      onSelect: handleDeleteResult,
                    })
                  }
                >
                  <Trash2 className="size-4" />
                  删除图书
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>

      <footer className="relative pb-8 text-center text-sm text-slate-500">
        Copyright © 2025 图书馆管理系统
      </footer>
    </main>
  )
}

function DetailItem({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof BookOpenText
  label: string
  value: string
}) {
  return (
    <div className="rounded-2xl border border-slate-200/80 bg-slate-50/70 px-4 py-4">
      <div className="mb-3 flex size-9 items-center justify-center rounded-xl bg-white text-slate-700 shadow-sm">
        <Icon className="size-4" />
      </div>
      <div className="space-y-1">
        <p className="text-sm text-slate-500">{label}</p>
        <p className="text-sm font-medium text-slate-900">{value}</p>
      </div>
    </div>
  )
}

function DetailText({ label, value }: { label: string; value: string }) {
  return (
    <div className="space-y-1">
      <p className="text-sm text-amber-700/80">{label}</p>
      <p className="text-sm font-medium text-slate-900">{value}</p>
    </div>
  )
}
