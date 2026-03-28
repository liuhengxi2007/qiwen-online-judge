import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { BookOpenText, LibraryBig, PencilLine, Plus, RotateCw, Search, Trash2 } from 'lucide-react'

import { FloatingPageTools } from '@/components/FloatingPageTools'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { BookCatalogRecord, BookInventoryStatuses } from '@/domain-types'
import { useMockSystem } from '@/hooks/useMockSystem'
import type { PageEventDefinition } from '@/lib/mock-types'

const pageName = '图书列表与管理页'
const route = '/library/admin/book/list'

const initialBooks: BookCatalogRecord[] = [
  new BookCatalogRecord('BK-1001', '活着', '余华', '文学', '9787506365437', BookInventoryStatuses.Available),
  new BookCatalogRecord('BK-1002', '三体', '刘慈欣', '科幻', '9787536692930', BookInventoryStatuses.Borrowed),
  new BookCatalogRecord('BK-1003', '人类简史', '尤瓦尔·赫拉利', '历史', '9787508647357', BookInventoryStatuses.Available),
  new BookCatalogRecord('BK-1004', '解忧杂货店', '东野圭吾', '小说', '9787544270878', BookInventoryStatuses.Borrowed),
  new BookCatalogRecord('BK-2001', '馆藏新编样书', '系统示例', '管理', '9787111888888', BookInventoryStatuses.Available),
]

export default function BookList() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { openMockDialog } = useMockSystem()

  const [books, setBooks] = useState<BookCatalogRecord[]>(initialBooks)
  const [keyword, setKeyword] = useState('')
  const [inlineMessage, setInlineMessage] = useState('')

  const highlightedId = searchParams.get('highlight')

  const filteredBooks = useMemo(() => {
    const trimmedKeyword = keyword.trim().toLowerCase()

    if (!trimmedKeyword) {
      return books
    }

    return books.filter((book) =>
        [book.title, book.author, book.categoryLabel, book.isbn, book.id]
        .join(' ')
        .toLowerCase()
        .includes(trimmedKeyword),
    )
  }, [books, keyword])

  const openDeleteDialog = (book: BookCatalogRecord) => {
    openMockDialog({
      pageName,
      route,
      componentName: `${book.title} 删除按钮`,
      interactionName: '删除图书',
      title: '选择删除处理结果',
      description: `模拟删除《${book.title}》后的页面反馈。`,
      options: [
        {
          id: 'delete-success',
          title: '删除成功',
          description: '图书从列表中移除。',
          badge: 'success',
          noticeMessage: `《${book.title}》已删除。`,
        },
        {
          id: 'delete-blocked',
          title: '删除被拦截',
          description: '该图书存在借阅记录，系统阻止删除。',
          badge: 'warning',
        },
      ],
      onSelect: (option) => {
        if (option.id === 'delete-success') {
          setInlineMessage('')
          setBooks((current) => current.filter((item) => item.id !== book.id))
          return
        }

        setInlineMessage(`《${book.title}》存在借阅记录，暂时无法删除。`)
      },
    })
  }

  const pageEvents: PageEventDefinition[] = [
    {
      id: 'page-load',
      label: '页面加载结果',
      description: '模拟管理员进入列表页后的数据加载状态。',
      dialog: {
        pageName,
        route,
        eventName: '页面加载结果',
        interactionName: '列表初始化加载',
        title: '选择页面加载状态',
        description: '模拟首次进入图书管理页时的数据反馈。',
        options: [
          {
            id: 'load-success',
            title: '加载成功',
            description: '正常显示当前图书列表。',
            badge: 'success',
          },
          {
            id: 'load-empty',
            title: '返回空列表',
            description: '页面展示为空状态，仅保留表头结构。',
            badge: 'warning',
          },
        ],
        onSelect: (option) => {
          setInlineMessage('')
          setBooks(option.id === 'load-empty' ? [] : initialBooks)
        },
      },
    },
    {
      id: 'refresh-list',
      label: '刷新列表',
      description: '模拟管理员点击刷新或重新拉取图书数据。',
      dialog: {
        pageName,
        route,
        eventName: '刷新列表',
        interactionName: '页面级数据刷新',
        title: '选择刷新结果',
        description: '模拟重新加载图书列表时的不同结果。',
        options: [
          {
            id: 'refresh-success',
            title: '刷新成功',
            description: '列表恢复到最新馆藏数据。',
            badge: 'success',
            noticeMessage: '图书列表已刷新。',
          },
          {
            id: 'refresh-error',
            title: '刷新失败',
            description: '保留当前列表，并提示稍后再试。',
            badge: 'error',
          },
        ],
        onSelect: (option) => {
          if (option.id === 'refresh-success') {
            setInlineMessage('')
            setBooks(initialBooks)
            return
          }

          setInlineMessage('图书列表刷新失败，请稍后再试。')
        },
      },
    },
  ]

  return (
    <main className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,rgba(185,208,255,0.36),transparent_36%),linear-gradient(180deg,#f5f7fb_0%,#eef2f7_48%,#e8edf4_100%)]">
      <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(255,255,255,0.56),transparent_42%,rgba(15,23,42,0.04)_100%)]" />
      <div className="absolute inset-x-0 top-0 h-72 bg-[radial-gradient(circle_at_center,rgba(15,23,42,0.12),transparent_70%)] opacity-70" />

      <FloatingPageTools events={pageEvents} onEventSelect={(event) => openMockDialog(event.dialog)} />

      <section className="relative mx-auto flex min-h-screen w-full max-w-7xl px-6 py-16 sm:px-8 lg:px-12">
        <div className="w-full space-y-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-xl space-y-4">
              <div className="inline-flex items-center gap-3 rounded-full border border-white/70 bg-white/75 px-4 py-2 text-sm text-slate-600 shadow-[0_12px_30px_rgba(15,23,42,0.08)] backdrop-blur">
                <LibraryBig className="size-4 text-slate-700" />
                图书管理
              </div>
              <div className="space-y-3">
                <h1 className="text-4xl font-semibold tracking-tight text-slate-900">
                  图书列表与管理
                </h1>
                <p className="text-base leading-7 text-slate-600">
                  查看馆藏图书，并通过正式页面继续完成新增、编辑、借书和还书流程。
                </p>
              </div>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row">
              <Button
                type="button"
                variant="outline"
                className="h-11 rounded-xl border-white/70 bg-white/80 px-5 text-slate-700 shadow-[0_12px_30px_rgba(15,23,42,0.06)] backdrop-blur"
                onClick={() => openMockDialog(pageEvents[1].dialog)}
              >
                <RotateCw className="mr-2 size-4" />
                刷新列表
              </Button>
              <Button
                type="button"
                className="h-11 rounded-xl bg-slate-900 px-5 text-white shadow-[0_16px_40px_rgba(15,23,42,0.18)] hover:bg-slate-800"
                onClick={() => navigate('/library/admin/book/add')}
              >
                <Plus className="mr-2 size-4" />
                添加图书
              </Button>
            </div>
          </div>

          <Card className="border-white/70 bg-white/82 py-0 shadow-[0_30px_80px_rgba(15,23,42,0.12)] backdrop-blur-xl">
            <CardHeader className="gap-4 border-b border-slate-200/70 px-7 py-7 sm:px-8">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div className="space-y-1">
                  <CardTitle className="text-2xl text-slate-900">馆藏图书列表</CardTitle>
                  <CardDescription className="text-sm text-slate-500">
                    按书名、作者、分类、ISBN 或编号搜索图书
                  </CardDescription>
                </div>

                <div className="relative w-full lg:w-80">
                  <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
                  <Input
                    value={keyword}
                    placeholder="搜索图书"
                    className="h-11 rounded-xl border-slate-200 bg-white pl-10 text-slate-900 placeholder:text-slate-400 focus-visible:ring-slate-400"
                    onChange={(event) => setKeyword(event.target.value)}
                  />
                </div>
              </div>
            </CardHeader>

            <CardContent className="space-y-5 px-7 py-7 sm:px-8">
              {highlightedId ? (
                <Alert className="rounded-2xl border-sky-200 bg-sky-50/90">
                  <AlertDescription className="text-sm text-sky-700">
                    已为你定位新图书记录 {highlightedId}，可继续查看详情或编辑信息。
                  </AlertDescription>
                </Alert>
              ) : null}

              {inlineMessage ? (
                <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/90">
                  <AlertDescription className="text-sm text-rose-700">{inlineMessage}</AlertDescription>
                </Alert>
              ) : null}

              <div className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white">
                <Table>
                  <TableHeader>
                    <TableRow className="border-slate-200 bg-slate-50/80 hover:bg-slate-50/80">
                      <TableHead className="px-4 py-3 text-slate-500">图书信息</TableHead>
                      <TableHead className="px-4 py-3 text-slate-500">分类</TableHead>
                      <TableHead className="px-4 py-3 text-slate-500">ISBN</TableHead>
                      <TableHead className="px-4 py-3 text-slate-500">状态</TableHead>
                      <TableHead className="px-4 py-3 text-right text-slate-500">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredBooks.length > 0 ? (
                      filteredBooks.map((book) => {
                        const isHighlighted = highlightedId === book.id

                        return (
                          <TableRow
                            key={book.id}
                            className={`border-slate-200/80 hover:bg-slate-50/60 ${isHighlighted ? 'bg-sky-50/70' : ''}`}
                          >
                            <TableCell className="px-4 py-4">
                              <div className="space-y-1">
                                <div className="flex items-center gap-3">
                                  <div className="flex size-10 items-center justify-center rounded-2xl bg-slate-900 text-white">
                                    <BookOpenText className="size-4" />
                                  </div>
                                  <div>
                                    <button
                                      type="button"
                                      className="font-medium text-slate-900 transition hover:text-slate-600"
                                      onClick={() => navigate(`/library/book/detail/${book.id}`)}
                                    >
                                      {book.title}
                                    </button>
                                    <p className="text-sm text-slate-500">
                                      {book.author} · {book.id}
                                    </p>
                                  </div>
                                </div>
                              </div>
                            </TableCell>
                            <TableCell className="px-4 py-4 text-slate-600">{book.categoryLabel}</TableCell>
                            <TableCell className="px-4 py-4 text-slate-600">{book.isbn}</TableCell>
                            <TableCell className="px-4 py-4">
                              <Badge
                                variant={book.status === 'available' ? 'secondary' : 'outline'}
                                className={
                                  book.status === BookInventoryStatuses.Available
                                    ? 'bg-emerald-50 text-emerald-700'
                                    : 'border-amber-200 bg-amber-50 text-amber-700'
                                }
                              >
                                {book.status === BookInventoryStatuses.Available ? '可借阅' : '已借出'}
                              </Badge>
                            </TableCell>
                            <TableCell className="px-4 py-4">
                              <div className="flex justify-end gap-2">
                                <Button
                                  type="button"
                                  variant="outline"
                                  className="rounded-xl"
                                  onClick={() => navigate(`/library/admin/book/edit/${book.id}`)}
                                >
                                  <PencilLine className="mr-2 size-4" />
                                  编辑
                                </Button>
                                {book.status === BookInventoryStatuses.Available ? (
                                  <Button
                                    type="button"
                                    variant="outline"
                                    className="rounded-xl"
                                    onClick={() =>
                                      navigate(`/library/admin/borrow/manage?bookId=${book.id}&bookName=${encodeURIComponent(book.title)}`)
                                    }
                                  >
                                    借出
                                  </Button>
                                ) : (
                                  <Button
                                    type="button"
                                    variant="outline"
                                    className="rounded-xl"
                                    onClick={() =>
                                      navigate(`/library/admin/return/manage?bookId=${book.id}&bookName=${encodeURIComponent(book.title)}`)
                                    }
                                  >
                                    归还
                                  </Button>
                                )}
                                <Button
                                  type="button"
                                  variant="ghost"
                                  className="rounded-xl text-rose-600 hover:bg-rose-50 hover:text-rose-700"
                                  onClick={() => openDeleteDialog(book)}
                                >
                                  <Trash2 className="mr-2 size-4" />
                                  删除
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        )
                      })
                    ) : (
                      <TableRow className="hover:bg-white">
                        <TableCell colSpan={5} className="px-4 py-16 text-center">
                          <div className="mx-auto max-w-sm space-y-3">
                            <div className="mx-auto flex size-14 items-center justify-center rounded-3xl bg-slate-100 text-slate-500">
                              <LibraryBig className="size-6" />
                            </div>
                            <div className="space-y-1">
                              <p className="text-base font-medium text-slate-900">暂无图书数据</p>
                              <p className="text-sm text-slate-500">请调整搜索条件或重新刷新列表。</p>
                            </div>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
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
