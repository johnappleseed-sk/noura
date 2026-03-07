import { Alert, Button, Card, Form, Input, Select, Space, Typography } from 'antd'
import { useEffect } from 'react'
import { useAdminDispatch, useAdminSelector } from '@/admin/app/hooks'
import {
  clearNotificationStatus,
  sendBroadcastNotification,
  sendUserNotification,
} from '@/admin/features/notifications/notificationsSlice'
import { fetchUsers } from '@/admin/features/users/usersSlice'
import { NotificationPayload } from '@/admin/types'

interface NotificationFormValues {
  targetUserId?: string
  category: NotificationPayload['category']
  title: string
  body: string
}

const categoryOptions = ['ORDER', 'SYSTEM', 'STORE', 'AI', 'SECURITY'].map((category) => ({
  label: category,
  value: category,
}))

/**
 * Renders the NotificationsPage component.
 *
 * @returns The rendered component tree.
 */
export const NotificationsPage = (): JSX.Element => {
  const dispatch = useAdminDispatch()
  const users = useAdminSelector((state) => state.users.items)
  const { sending, successMessage, error } = useAdminSelector((state) => state.notifications)

  useEffect(() => {
    void dispatch(fetchUsers())
    return () => {
      dispatch(clearNotificationStatus())
    }
  }, [dispatch])

  /**
   * Executes on broadcast.
   *
   * @param values The values value.
   * @returns No value.
   */
  const onBroadcast = async (values: NotificationFormValues): Promise<void> => {
    await dispatch(
      sendBroadcastNotification({
        category: values.category,
        title: values.title,
        body: values.body,
      }),
    )
  }

  /**
   * Executes on targeted.
   *
   * @param values The values value.
   * @returns No value.
   */
  const onTargeted = async (values: NotificationFormValues): Promise<void> => {
    if (!values.targetUserId) {
      return
    }
    await dispatch(
      sendUserNotification({
        userId: values.targetUserId,
        payload: {
          targetUserId: values.targetUserId,
          category: values.category,
          title: values.title,
          body: values.body,
        },
      }),
    )
  }

  return (
    <>
      <Typography.Title level={3}>Notification Sender</Typography.Title>
      {successMessage ? <Alert showIcon message={successMessage} style={{ marginBottom: 12 }} type="success" /> : null}
      {error ? <Alert showIcon message={error} style={{ marginBottom: 12 }} type="error" /> : null}
      <Space align="start" size={16} wrap>
        <Card title="Broadcast" style={{ width: 420 }}>
          <Form<NotificationFormValues> layout="vertical" onFinish={onBroadcast}>
            <Form.Item initialValue="SYSTEM" label="Category" name="category" rules={[{ required: true }]}>
              <Select options={categoryOptions} />
            </Form.Item>
            <Form.Item label="Title" name="title" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item label="Body" name="body" rules={[{ required: true }]}>
              <Input.TextArea rows={4} />
            </Form.Item>
            <Button htmlType="submit" loading={sending} type="primary">
              Send Broadcast
            </Button>
          </Form>
        </Card>
        <Card title="Targeted User Notification" style={{ width: 420 }}>
          <Form<NotificationFormValues> layout="vertical" onFinish={onTargeted}>
            <Form.Item label="User" name="targetUserId" rules={[{ required: true }]}>
              <Select
                options={users.map((user) => ({
                  label: `${user.fullName} (${user.email})`,
                  value: user.id,
                }))}
                showSearch
              />
            </Form.Item>
            <Form.Item initialValue="ORDER" label="Category" name="category" rules={[{ required: true }]}>
              <Select options={categoryOptions} />
            </Form.Item>
            <Form.Item label="Title" name="title" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item label="Body" name="body" rules={[{ required: true }]}>
              <Input.TextArea rows={4} />
            </Form.Item>
            <Button htmlType="submit" loading={sending} type="primary">
              Send to User
            </Button>
          </Form>
        </Card>
      </Space>
    </>
  )
}
