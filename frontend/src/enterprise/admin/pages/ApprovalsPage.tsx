import { Button, Input, Select, Space, Table, Tag, Typography, message } from 'antd'
import { useEffect, useState } from 'react'
import { useAdminDispatch, useAdminSelector } from '@/admin/app/hooks'
import { fetchApprovals, updateApproval } from '@/admin/features/approvals/approvalsSlice'
import { Approval } from '@/admin/types'

/**
 * Renders the ApprovalsPage component.
 *
 * @returns The rendered component tree.
 */
export const ApprovalsPage = (): JSX.Element => {
  const dispatch = useAdminDispatch()
  const { items, loading, saving } = useAdminSelector((state) => state.approvals)
  const [notesById, setNotesById] = useState<Record<string, string>>({})
  const [statusById, setStatusById] = useState<Record<string, Approval['status']>>({})

  useEffect(() => {
    void dispatch(fetchApprovals())
  }, [dispatch])

  /**
   * Executes submit decision.
   *
   * @param record The record value.
   * @returns No value.
   */
  const submitDecision = async (record: Approval): Promise<void> => {
    const status = statusById[record.id] ?? record.status
    const notes = notesById[record.id] ?? record.reviewerNotes ?? ''
    await dispatch(updateApproval({ id: record.id, status, notes }))
    message.success('Approval decision saved')
  }

  return (
    <>
      <Typography.Title level={3}>B2B Approval Panel</Typography.Title>
      <Table<Approval>
        rowKey="id"
        loading={loading}
        dataSource={items}
        pagination={{ pageSize: 20 }}
        columns={[
          { title: 'Approval ID', dataIndex: 'id', render: (value: string) => value.slice(0, 12) },
          { title: 'Requester', dataIndex: 'requesterId', render: (value: string) => value.slice(0, 12) },
          { title: 'Order', dataIndex: 'orderId', render: (value: string | null) => (value ? value.slice(0, 12) : '-') },
          { title: 'Amount', dataIndex: 'amount', render: (value: number) => `$${value.toFixed(2)}` },
          { title: 'Current Status', render: (_, record) => <Tag>{record.status}</Tag> },
          {
            title: 'Decision',
            render: (_, record) => (
              <Space direction="vertical">
                <Select<Approval['status']>
                  options={[
                    { label: 'PENDING', value: 'PENDING' },
                    { label: 'APPROVED', value: 'APPROVED' },
                    { label: 'REJECTED', value: 'REJECTED' },
                  ]}
                  style={{ width: 140 }}
                  value={statusById[record.id] ?? record.status}
                  onChange={(value) => setStatusById((state) => ({ ...state, [record.id]: value }))}
                />
                <Input
                  placeholder="Reviewer notes"
                  value={notesById[record.id] ?? record.reviewerNotes ?? ''}
                  onChange={(event) => setNotesById((state) => ({ ...state, [record.id]: event.target.value }))}
                />
                <Button loading={saving} size="small" type="primary" onClick={() => void submitDecision(record)}>
                  Save
                </Button>
              </Space>
            ),
          },
        ]}
      />
    </>
  )
}
