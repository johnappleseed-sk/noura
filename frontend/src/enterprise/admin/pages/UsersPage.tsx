import { Space, Table, Tag, Typography } from 'antd'
import { useEffect } from 'react'
import { useAdminDispatch, useAdminSelector } from '@/admin/app/hooks'
import { fetchUsers } from '@/admin/features/users/usersSlice'
import { UserProfile } from '@/admin/types'

/**
 * Renders the UsersPage component.
 *
 * @returns The rendered component tree.
 */
export const UsersPage = (): JSX.Element => {
  const dispatch = useAdminDispatch()
  const { items, loading } = useAdminSelector((state) => state.users)

  useEffect(() => {
    void dispatch(fetchUsers())
  }, [dispatch])

  return (
    <>
      <Typography.Title level={3}>User Management</Typography.Title>
      <Table<UserProfile>
        rowKey="id"
        loading={loading}
        dataSource={items}
        pagination={{ pageSize: 20 }}
        columns={[
          { title: 'Full Name', dataIndex: 'fullName' },
          { title: 'Email', dataIndex: 'email' },
          { title: 'Phone', dataIndex: 'phone', render: (phone: string | null) => phone ?? 'N/A' },
          {
            title: 'Roles',
            render: (_, record) => (
              <Space>
                {record.roles.map((role) => (
                  <Tag key={role} color={role === 'ADMIN' ? 'red' : role === 'B2B' ? 'blue' : 'default'}>
                    {role}
                  </Tag>
                ))}
              </Space>
            ),
          },
          { title: 'Preferred Store', dataIndex: 'preferredStoreId', render: (value: string | null) => value ?? '-' },
        ]}
      />
    </>
  )
}
