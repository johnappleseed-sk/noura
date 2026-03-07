import { App as AntApp, Button, Card, Input, Select, Space, Table, Tag } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '@/api/adminApi';
import { PageHeader } from '@/components/PageHeader';
import type { Approval, ApprovalStatus } from '@/types/models';

const APPROVAL_STATUSES: ApprovalStatus[] = ['PENDING', 'APPROVED', 'REJECTED'];

interface ApprovalDraft {
  status: ApprovalStatus;
  reviewerNotes: string;
}

/**
 * Renders the ApprovalsPage component.
 *
 * @returns The rendered component tree.
 */
export const ApprovalsPage = (): JSX.Element => {
  const { message } = AntApp.useApp();
  const [approvals, setApprovals] = useState<Approval[]>([]);
  const [loading, setLoading] = useState(false);
  const [drafts, setDrafts] = useState<Record<string, ApprovalDraft>>({});

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await adminApi.getApprovals();
      setApprovals(result);
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to load approvals');
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void load();
  }, [load]);

  /**
   * Executes draft for.
   *
   * @param approval The approval value.
   * @returns The result of draft for.
   */
  const draftFor = (approval: Approval): ApprovalDraft =>
    drafts[approval.id] ?? { status: approval.status, reviewerNotes: approval.reviewerNotes ?? '' };

  /**
   * Executes save.
   *
   * @param approval The approval value.
   * @returns No value.
   */
  const save = async (approval: Approval): Promise<void> => {
    const draft = draftFor(approval);
    try {
      await adminApi.updateApproval(approval.id, draft.status, draft.reviewerNotes);
      message.success('Approval updated');
      await load();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to update approval');
    }
  };

  return (
    <>
      <PageHeader
        title="B2B Approval Queue"
        subtitle="Review pending approval requests, set decision states, and capture reviewer context."
      />

      <Card className="admin-section-card admin-data-card">
        <Table<Approval>
          rowKey="id"
          loading={loading}
          dataSource={approvals}
          pagination={false}
          scroll={{ x: 1060 }}
          columns={[
            { title: 'Approval ID', dataIndex: 'id', width: 230 },
            { title: 'Requester', dataIndex: 'requesterId', width: 230 },
            {
              title: 'Amount',
              dataIndex: 'amount',
              render: (value: number) => `$${Number(value).toFixed(2)}`,
            },
            {
              title: 'Current Status',
              render: (_, record) => <Tag>{record.status}</Tag>,
            },
            {
              title: 'Review',
              width: 420,
              render: (_, record) => (
                <Space direction="vertical" style={{ width: '100%' }}>
                  <Select<ApprovalStatus>
                    value={draftFor(record).status}
                    options={APPROVAL_STATUSES.map((status) => ({ label: status, value: status }))}
                    onChange={(status) =>
                      setDrafts((current) => ({
                        ...current,
                        [record.id]: {
                          ...draftFor(record),
                          status,
                        },
                      }))
                    }
                  />
                  <Input.TextArea
                    rows={2}
                    value={draftFor(record).reviewerNotes}
                    onChange={(event) =>
                      setDrafts((current) => ({
                        ...current,
                        [record.id]: {
                          ...draftFor(record),
                          reviewerNotes: event.target.value,
                        },
                      }))
                    }
                    placeholder="Reviewer notes"
                  />
                  <Button type="primary" onClick={() => void save(record)}>
                    Save
                  </Button>
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </>
  );
};
