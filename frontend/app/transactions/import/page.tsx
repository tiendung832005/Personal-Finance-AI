'use client'

import { ChangeEvent, DragEvent, useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Header } from '@/components/layout/header'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { apiFetch, ApiError } from '@/lib/api'
import { useToast } from '@/hooks/use-toast'

type Category = { id: number; name: string; type: 'EXPENSE' | 'INCOME' | 'BOTH' }
type Account = { id: number; name: string }
type PreviewRow = {
  rowNumber: number
  date: string
  amount: number
  type: 'INCOME' | 'EXPENSE'
  description: string | null
  note: string | null
  categoryId: number | null
  categoryName: string | null
  autoCategorized: boolean
}
type ErrorRow = { rowNumber: number; rawData: string; error: string }
type PreviewResponse = { validCount: number; errorCount: number; validRows: PreviewRow[]; errorRows: ErrorRow[] }

export default function ImportPage() {
  const router = useRouter()
  const { toast } = useToast()

  const [step, setStep] = useState<1 | 2 | 3>(1)
  const [file, setFile] = useState<File | null>(null)
  const [isParsing, setIsParsing] = useState(false)
  const [isConfirming, setIsConfirming] = useState(false)
  const [preview, setPreview] = useState<PreviewResponse | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [accountId, setAccountId] = useState<string>('')
  const [importedCount, setImportedCount] = useState(0)

  useEffect(() => {
    ;(async () => {
      try {
        const [catRes, accRes] = await Promise.all([
          apiFetch<Category[]>('/api/categories', { method: 'GET' }),
          apiFetch<Account[]>('/api/accounts', { method: 'GET' }),
        ])
        setCategories(catRes.data || [])
        const allAccounts = accRes.data || []
        setAccounts(allAccounts)
        if (allAccounts.length > 0) {
          setAccountId(String(allAccounts[0].id))
        }
      } catch {
        toast({ title: 'Không tải được dữ liệu', variant: 'destructive' })
      }
    })()
  }, [toast])

  const allCategorized = useMemo(
    () => (preview?.validRows || []).every((row) => !!row.categoryId),
    [preview]
  )

  const validateCsvFile = (selected: File | null): selected is File => {
    if (!selected) return false
    if (!selected.name.toLowerCase().endsWith('.csv')) {
      toast({ title: 'Chỉ chấp nhận file .csv', variant: 'destructive' })
      return false
    }
    return true
  }

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    const selected = e.dataTransfer.files?.[0] || null
    if (validateCsvFile(selected)) {
      setFile(selected)
    }
  }

  const onPickFile = (e: ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0] || null
    if (validateCsvFile(selected)) {
      setFile(selected)
    }
  }

  const handleUpload = async () => {
    if (!file) return
    const formData = new FormData()
    formData.append('file', file)

    setIsParsing(true)
    try {
      const res = await apiFetch<PreviewResponse>('/api/v1/transactions/import', {
        method: 'POST',
        body: formData,
      })
      setPreview(res.data || null)
      setStep(2)
    } catch (err) {
      toast({
        title: 'Import thất bại',
        description: err instanceof ApiError ? err.message : 'Không thể parse file CSV',
        variant: 'destructive',
      })
    } finally {
      setIsParsing(false)
    }
  }

  const updateCategory = (rowNumber: number, nextCategoryId: string) => {
    if (!preview) return
    const categoryIdNum = Number(nextCategoryId)
    const category = categories.find((c) => c.id === categoryIdNum)

    setPreview({
      ...preview,
      validRows: preview.validRows.map((row) =>
        row.rowNumber === rowNumber
          ? {
              ...row,
              categoryId: categoryIdNum,
              categoryName: category?.name || null,
              autoCategorized: false,
            }
          : row
      ),
    })
  }

  const handleConfirm = async () => {
    if (!preview || !accountId || !allCategorized || preview.validRows.length === 0) return

    setIsConfirming(true)
    try {
      const payload = {
        accountId: Number(accountId),
        validRows: preview.validRows.map((r) => ({
          rowNumber: r.rowNumber,
          date: r.date,
          amount: r.amount,
          type: r.type,
          description: r.description,
          note: r.note,
          categoryId: r.categoryId,
          autoCategorized: r.autoCategorized,
        })),
      }

      const res = await apiFetch<{ importedCount: number }>('/api/v1/transactions/import/confirm', {
        method: 'POST',
        body: JSON.stringify(payload),
      })

      setImportedCount(res.data?.importedCount || 0)
      setStep(3)
    } catch (err) {
      toast({
        title: 'Xác nhận import thất bại',
        description: err instanceof ApiError ? err.message : 'Vui lòng thử lại',
        variant: 'destructive',
      })
    } finally {
      setIsConfirming(false)
    }
  }

  const downloadTemplate = () => {
    const csv = [
      'date,amount,type,description,note',
      '2026-04-01,50000,EXPENSE,Grab di làm,',
      '2026-04-02,150000,EXPENSE,Siêu thị VinMart,',
      '2026-04-03,5000000,INCOME,Luong tháng 4,',
      '2026-04-04,200000,EXPENSE,FT26104123456,',
    ].join('\n')

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'template-transactions.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <DashboardLayout>
      <Header title='Import giao dịch CSV' subtitle='Nhập giao dịch nhanh từ file CSV' />
      <div className='p-6 space-y-6'>
        {step === 1 && (
          <div className='rounded-xl border p-6 space-y-4'>
            <p className='text-sm text-muted-foreground'>
              File CSV cần có các cột: date, amount, type, description, note
            </p>
            <div
              onDrop={onDrop}
              onDragOver={(e) => e.preventDefault()}
              className='rounded-lg border-2 border-dashed p-8 text-center'
            >
              <p>{file ? `Ðã chọn: ${file.name}` : 'Kéo thả file CSV vào dây hoăc chọn file'}</p>
              <input className='mt-4' type='file' accept='.csv' onChange={onPickFile} />
            </div>
            <div className='flex flex-wrap gap-3'>
              <Button variant='outline' onClick={downloadTemplate}>Tải template CSV mẫu</Button>
              <Button variant='outline' onClick={() => router.push('/transactions')}>Hủy</Button>
              <Button onClick={handleUpload} disabled={!file || isParsing}>
                {isParsing ? 'Ðang parse...' : 'Upload và xem truớc'}
              </Button>
            </div>
          </div>
        )}

        {step === 2 && preview && (
          <div className='space-y-4'>
            <div className='flex flex-wrap gap-4 items-center'>
              <Badge className='bg-green-600'>{preview.validCount} giao dịch hợp lệ</Badge>
              <Badge variant='destructive'>{preview.errorCount} giao dịch lỗi</Badge>
              <Select value={accountId} onValueChange={setAccountId}>
                <SelectTrigger className='w-[260px]'>
                  <SelectValue placeholder='Chọn tài khoản nhận giao dịch' />
                </SelectTrigger>
                <SelectContent>
                  {accounts.map((a) => (
                    <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className='rounded-xl border overflow-auto'>
              <table className='w-full text-sm'>
                <thead className='bg-muted'>
                  <tr>
                    <th className='p-2 text-left'>Ngày</th>
                    <th className='p-2 text-right'>Số tiền</th>
                    <th className='p-2'>Loại</th>
                    <th className='p-2'>Mô tả</th>
                    <th className='p-2'>Danh mục</th>
                  </tr>
                </thead>
                <tbody>
                  {preview.validRows.map((row) => (
                    <tr key={row.rowNumber} className='border-t'>
                      <td className='p-2'>{new Date(row.date).toLocaleDateString('vi-VN')}</td>
                      <td className='p-2 text-right'>{Number(row.amount).toLocaleString('vi-VN')} đ</td>
                      <td className='p-2 text-center'>{row.type}</td>
                      <td className='p-2'>{row.description || '(trống)'}</td>
                      <td className={`p-2 ${!row.categoryId ? 'bg-yellow-50' : ''}`}>
                        {row.categoryId ? (
                          <div className='flex items-center gap-2'>
                            <span>{row.categoryName}</span>
                            {row.autoCategorized && <Badge className='bg-green-600'>AI gợi ý</Badge>}
                          </div>
                        ) : (
                          <Select onValueChange={(v) => updateCategory(row.rowNumber, v)}>
                            <SelectTrigger>
                              <SelectValue placeholder='Chọn danh mục' />
                            </SelectTrigger>
                            <SelectContent>
                              {categories
                                .filter((c) => c.type === row.type || c.type === 'BOTH')
                                .map((c) => (
                                  <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>
                                ))}
                            </SelectContent>
                          </Select>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {preview.validRows.length === 0 && (
              <div className='rounded-xl border border-yellow-300 bg-yellow-50 p-4 text-sm text-yellow-800'>
                Không có dòng hợp lệ để import. Vui lòng kiểm tra lại và upload lại file khác.
              </div>
            )}

            {preview.errorRows.length > 0 && (
              <div className='rounded-xl border border-red-200 bg-red-50 p-4'>
                <p className='font-semibold text-red-700'>Danh sách l?i</p>
                <ul className='mt-2 space-y-1 text-sm text-red-700'>
                  {preview.errorRows.map((e) => (
                    <li key={e.rowNumber}>Dòng {e.rowNumber}: {e.error}</li>
                  ))}
                </ul>
              </div>
            )}

            <div className='flex gap-3'>
              <Button
                variant='outline'
                onClick={() => {
                  setStep(1)
                  setPreview(null)
                }}
              >
                Hu?
              </Button>
              <Button
                onClick={handleConfirm}
                disabled={!allCategorized || isConfirming || preview.validRows.length === 0}
              >
                {isConfirming ? 'Ðang import...' : 'Xác nhận import'}
              </Button>
            </div>
          </div>
        )}

        {step === 3 && (
          <div className='rounded-xl border p-8 text-center space-y-4'>
            <p className='text-xl font-semibold'>Ðã import thành công {importedCount} giao dịch</p>
            <Button onClick={() => router.push('/transactions')}>Xem danh sách giao dịch</Button>
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
