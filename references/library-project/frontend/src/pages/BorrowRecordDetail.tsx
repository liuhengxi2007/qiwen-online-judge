import { useMemo } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ArrowLeft, BookCopy, CalendarClock, FileSearch, UserRound } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { BorrowRecordEntity, BorrowRecordStatuses, type BorrowRecordStatus } from '@/domain-types'

const recordMap = {
  'BR-2025-0318': {
    record: new BorrowRecordEntity('BR-2025-0318', '深入理解计算机系统', '李晨', '2025-03-18', '2025-04-01', BorrowRecordStatuses.Borrowing),
    process: '已完成借书登记，等待按期归还。',
  },
  'BR-2025-0312': {
    record: new BorrowRecordEntity('BR-2025-0312', '算法导论', '周宁', '2025-03-12', '2025-03-26', BorrowRecordStatuses.Overdue),
    process: '记录已进入逾期状态，管理员可继续跟进归还。',
  },
  'BR-2025-0305': {
    record: new BorrowRecordEntity('BR-2025-0305', '设计模式', '王雪', '2025-03-05', '2025-03-19', BorrowRecordStatuses.Returned, '2025-03-18'),
    process: '归还流程已完成，可回看借阅与归还时间。',
  },
  'BR-2025-0322': {
    record: new BorrowRecordEntity('BR-2025-0322', '数据库系统概念', '赵晴', '2025-03-22', '2025-04-05', BorrowRecordStatuses.Borrowing),
    process: '这是一条新建借阅记录，可继续查看详情或回到列表跟进。',
  },
  'BR-2025-0416': {
    record: new BorrowRecordEntity('BR-2025-0416', '百年孤独', '代办读者', '2025-04-16', '2025-04-30', BorrowRecordStatuses.Borrowing),
    process: '该记录由图书详情页直接发起借书后生成，可在此继续核对借阅信息。',
  },
  'RT-2025-0416': {
    record: new BorrowRecordEntity('RT-2025-0416', '百年孤独', '李晨', '2025-03-18', '2025-04-01', BorrowRecordStatuses.Returned, '2025-04-16'),
    process: '该记录对应图书详情页发起的还书结果，可继续返回还书管理页核对。',
  },
} as const

export default function BorrowRecordDetail() {
  const navigate = useNavigate()
  const { id } = useParams()
  const [searchParams] = useSearchParams()

  const record = useMemo(() => {
    if (id && id in recordMap) {
      const detail = recordMap[id as keyof typeof recordMap]
      return {
        ...detail.record,
        process: detail.process,
      }
    }

    return {
      id: id ?? 'BR-UNKNOWN',
      bookName: searchParams.get('bookName') ?? '图书借阅记录',
      readerName: searchParams.get('readerName') ?? '待确认读者',
      borrowDate: searchParams.get('borrowDate') ?? '2025-04-16',
      dueDate: searchParams.get('dueDate') ?? '2025-04-30',
      returnDate: searchParams.get('returnDate') ?? '',
      status: (searchParams.get('status') as BorrowRecordStatus | null) ?? BorrowRecordStatuses.Borrowing,
      process: '当前记录由页面承接生成，可继续回到业务页面跟进。',
    }
  }, [id, searchParams])

  return (
    <main className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,rgba(185,208,255,0.36),transparent_36%),linear-gradient(180deg,#f5f7fb_0%,#eef2f7_48%,#e8edf4_100%)]">
      <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.56),transparent_42%,rgba(15,23,42,0.04)_100%)]" />
      <div className="absolute inset-x-0 top-0 h-72 bg-[radial-gradient(circle_at_center,rgba(15,23,42,0.12),transparent_70%)] opacity-70" />

      <section className="relative mx-auto flex min-h-screen w-full max-w-6xl items-center justify-center px-6 py-16 sm:px-8 lg:px-12">
        <div className="grid w-full max-w-5xl gap-8 lg:grid-cols-[0.9fr_1.1fr]">
          <div className="hidden lg:block">
            <div className="max-w-md space-y-4">
              <div className="inline-flex items-center gap-3 rounded-full border border-white/70 bg-white/75 px-4 py-2 text-sm text-slate-600 shadow-[0_12px_30px_rgba(15,23,42,0.08)] backdrop-blur">
                <FileSearch className="size-4 text-slate-700" />
                借阅记录详情
              </div>
              <div className="space-y-4">
                <h1 className="text-4xl font-semibold tracking-tight text-slate-900">
                  图书馆管理系统 - 借阅记录详情
                </h1>
                <p className="text-base leading-7 text-slate-600">
                  在这里核对单条借阅或归还记录，并回到对应业务页面继续处理。
                </p>
              </div>
            </div>
          </div>

          <Card className="border-white/70 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-4 border-b border-slate-200/70 px-7 py-7 sm:px-8">
              <div className="flex items-start justify-between gap-4">
                <div className="space-y-2">
                  <CardTitle className="text-2xl text-slate-900">{record.id}</CardTitle>
                  <CardDescription className="text-sm text-slate-500">
                    单条借阅记录核对页
                  </CardDescription>
                </div>
                <Badge
                  variant="outline"
                  className={
                    record.status === BorrowRecordStatuses.Borrowing
                      ? 'border-sky-200 bg-sky-50 text-sky-700'
                      : record.status === BorrowRecordStatuses.Overdue
                        ? 'border-amber-200 bg-amber-50 text-amber-700'
                        : 'border-emerald-200 bg-emerald-50 text-emerald-700'
                  }
                >
                  {record.status}
                </Badge>
              </div>
            </CardHeader>

            <CardContent className="space-y-6 px-7 py-7 sm:px-8">
              <div className="grid gap-4 sm:grid-cols-2">
                <DetailCard icon={BookCopy} label="图书名称" value={record.bookName} />
                <DetailCard icon={UserRound} label="借阅人" value={record.readerName} />
                <DetailCard icon={CalendarClock} label="借出日期" value={record.borrowDate} />
                <DetailCard icon={CalendarClock} label="应还日期" value={record.dueDate} />
              </div>

              <div className="rounded-3xl border border-slate-200/80 bg-slate-50/80 p-5">
                <p className="text-sm text-slate-500">归还日期</p>
                <p className="mt-1 text-sm font-medium text-slate-900">
                  {record.returnDate || '尚未归还'}
                </p>
              </div>

              <div className="rounded-3xl border border-slate-200/80 bg-white/80 p-5">
                <p className="text-sm text-slate-500">处理说明</p>
                <p className="mt-2 text-sm leading-7 text-slate-700">{record.process}</p>
              </div>

              <div className="flex flex-wrap gap-3">
                <Button
                  type="button"
                  className="h-11 rounded-xl bg-slate-900 px-5 text-white hover:bg-slate-800"
                  onClick={() => navigate('/library/admin/borrow/manage')}
                >
                  返回借书管理
                </Button>
                {record.status === BorrowRecordStatuses.Returned || record.id.startsWith('RT-') ? (
                  <Button
                    type="button"
                    variant="outline"
                    className="h-11 rounded-xl"
                    onClick={() => navigate('/library/admin/return/manage')}
                  >
                    前往还书管理
                  </Button>
                ) : null}
                <Button
                  type="button"
                  variant="outline"
                  className="h-11 rounded-xl"
                  onClick={() => navigate('/library/admin/book/list')}
                >
                  <ArrowLeft className="mr-2 size-4" />
                  返回图书列表
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  )
}

function DetailCard({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof BookCopy
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
