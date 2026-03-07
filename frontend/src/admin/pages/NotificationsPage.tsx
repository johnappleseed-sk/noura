import { App as AntApp, Button, Card, Form, Input, Radio, Select, Typography } from 'antd';
import { adminApi } from '@/api/adminApi';
import { PageHeader } from '@/components/PageHeader';

type NotificationScope = 'broadcast' | 'user';
type NotificationCategory = 'ORDER' | 'SYSTEM' | 'STORE' | 'AI' | 'SECURITY';

interface NotificationFormValues {
  scope: NotificationScope;
  userId?: string;
  category: NotificationCategory;
  title: string;
  body: string;
}

/**
 * Renders the NotificationsPage component.
 *
 * @returns The rendered component tree.
 */
export const NotificationsPage = (): JSX.Element => {
  const { message } = AntApp.useApp();
  const [form] = Form.useForm<NotificationFormValues>();

  /**
   * Executes submit.
   *
   * @returns No value.
   */
  const submit = async (): Promise<void> => {
    const values = await form.validateFields();
    try {
      if (values.scope === 'user' && values.userId) {
        await adminApi.sendUserNotification(values.userId, {
          category: values.category,
          title: values.title,
          body: values.body,
        });
      } else {
        await adminApi.sendBroadcast({
          category: values.category,
          title: values.title,
          body: values.body,
        });
      }
      message.success('Notification sent');
      form.resetFields();
      form.setFieldValue('scope', 'broadcast');
      form.setFieldValue('category', 'SYSTEM');
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to send notification');
    }
  };

  return (
    <>
      <PageHeader
        title="Notification Sender"
        subtitle="Publish real-time announcements to all users or target individual recipients."
      />

      <Card className="admin-section-card" title="Compose Notification">
        <Typography.Paragraph type="secondary">
          Use this tool for operational alerts, system notices, or user-specific escalations.
        </Typography.Paragraph>
        <Form<NotificationFormValues>
          form={form}
          layout="vertical"
          initialValues={{ scope: 'broadcast', category: 'SYSTEM', title: '', body: '' }}
          onFinish={() => {
            void submit();
          }}
        >
          <Form.Item name="scope" label="Target Scope" rules={[{ required: true }]}>
            <Radio.Group
              options={[
                { label: 'Broadcast', value: 'broadcast' },
                { label: 'Single User', value: 'user' },
              ]}
            />
          </Form.Item>
          <Form.Item shouldUpdate noStyle>
            {() =>
              form.getFieldValue('scope') === 'user' ? (
                <Form.Item name="userId" label="Target User ID" rules={[{ required: true }]}>
                  <Input placeholder="UUID user id" />
                </Form.Item>
              ) : null
            }
          </Form.Item>
          <Form.Item name="category" label="Category" rules={[{ required: true }]}>
            <Select
              options={[
                { label: 'ORDER', value: 'ORDER' },
                { label: 'SYSTEM', value: 'SYSTEM' },
                { label: 'STORE', value: 'STORE' },
                { label: 'AI', value: 'AI' },
                { label: 'SECURITY', value: 'SECURITY' },
              ]}
            />
          </Form.Item>
          <Form.Item name="title" label="Title" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="body" label="Message" rules={[{ required: true }]}>
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">
              Send Notification
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </>
  );
};
