import { EditOutlined } from '@ant-design/icons';
import { App as AntApp, Button, Card, Checkbox, Modal, Space, Switch, Table, Tag } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '@/api/adminApi';
import { PageHeader } from '@/components/PageHeader';
import type { PageResponse } from '@/types/api';
import type { RoleType, UserProfile } from '@/types/models';

const ROLE_OPTIONS: RoleType[] = ['ADMIN', 'CUSTOMER', 'B2B'];

interface UserDraft {
  roles: RoleType[];
  enabled: boolean;
}

/**
 * Renders the UsersPage component.
 *
 * @returns The rendered component tree.
 */
export const UsersPage = (): JSX.Element => {
  const { message } = AntApp.useApp();
  const [pageData, setPageData] = useState<PageResponse<UserProfile> | null>(null);
  const [loading, setLoading] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserProfile | null>(null);
  const [draft, setDraft] = useState<UserDraft | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await adminApi.getUsers({ page: 0, size: 50, sortBy: 'createdAt', direction: 'desc' });
      setPageData(result);
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void load();
  }, [load]);

  /**
   * Executes open editor.
   *
   * @param user The user context for this operation.
   * @returns No value.
   */
  const openEditor = (user: UserProfile): void => {
    setSelectedUser(user);
    setDraft({ roles: user.roles, enabled: user.enabled });
  };

  /**
   * Executes save.
   *
   * @returns No value.
   */
  const save = async (): Promise<void> => {
    if (!selectedUser || !draft) {
      return;
    }
    try {
      await adminApi.updateUser(selectedUser.id, { roles: draft.roles, enabled: draft.enabled });
      message.success('User updated');
      setSelectedUser(null);
      setDraft(null);
      await load();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to update user');
    }
  };

  return (
    <>
      <PageHeader
        title="User Management"
        subtitle="Manage user roles and account status to enforce operational access policies."
      />

      <Card className="admin-section-card admin-data-card">
        <Table<UserProfile>
          rowKey="id"
          loading={loading}
          dataSource={pageData?.content ?? []}
          pagination={false}
          scroll={{ x: 800 }}
          columns={[
            { title: 'Full Name', dataIndex: 'fullName' },
            { title: 'Email', dataIndex: 'email' },
            {
              title: 'Roles',
              render: (_, record) => (
                <>
                  {record.roles.map((role) => (
                    <Tag key={role}>{role}</Tag>
                  ))}
                </>
              ),
            },
            { title: 'Enabled', render: (_, record) => (record.enabled ? 'Yes' : 'No') },
            {
              title: 'Actions',
              render: (_, record) => (
                <Button icon={<EditOutlined />} onClick={() => openEditor(record)}>
                  Edit
                </Button>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title="Edit User"
        open={Boolean(selectedUser && draft)}
        onCancel={() => {
          setSelectedUser(null);
          setDraft(null);
        }}
        onOk={() => {
          void save();
        }}
      >
        {draft ? (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Checkbox.Group<RoleType>
              options={ROLE_OPTIONS.map((role) => ({ label: role, value: role }))}
              value={draft.roles}
              onChange={(roles) => setDraft((current) => (current ? { ...current, roles } : current))}
            />
            <Space>
              <span>Enabled</span>
              <Switch
                checked={draft.enabled}
                onChange={(enabled) => setDraft((current) => (current ? { ...current, enabled } : current))}
              />
            </Space>
          </Space>
        ) : null}
      </Modal>
    </>
  );
};
