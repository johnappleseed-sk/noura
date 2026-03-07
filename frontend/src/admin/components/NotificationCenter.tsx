import { BellOutlined } from '@ant-design/icons';
import { Badge, Button, List, Popover, Space, Tag, Typography } from 'antd';
import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import {
  fetchNotifications,
  fetchUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
  selectNotifications,
  selectNotificationsState,
  selectUnreadCount,
  setPanelOpen,
} from '@/features/notifications/notificationsSlice';

const categoryColor: Record<string, string> = {
  ORDER: 'blue',
  SYSTEM: 'purple',
  STORE: 'geekblue',
  AI: 'cyan',
  SECURITY: 'red',
};

/**
 * Renders the NotificationCenter component.
 *
 * @returns The rendered component tree.
 */
export const NotificationCenter = (): JSX.Element => {
  const dispatch = useAppDispatch();
  const notifications = useAppSelector(selectNotifications);
  const unreadCount = useAppSelector(selectUnreadCount);
  const { panelOpen, status } = useAppSelector(selectNotificationsState);

  useEffect(() => {
    void dispatch(fetchUnreadCount());
    const timer = window.setInterval(() => {
      void dispatch(fetchUnreadCount());
    }, 10000);
    return () => window.clearInterval(timer);
  }, [dispatch]);

  useEffect(() => {
    if (!panelOpen) {
      return;
    }
    void dispatch(fetchNotifications());
    const timer = window.setInterval(() => {
      void dispatch(fetchNotifications());
    }, 8000);
    return () => window.clearInterval(timer);
  }, [dispatch, panelOpen]);

  const content = (
    <div className="admin-notif-popover">
      <Space style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
        <Typography.Text strong>Notifications</Typography.Text>
        <Button
          size="small"
          type="link"
          onClick={() => {
            void dispatch(markAllNotificationsRead());
          }}
        >
          Mark all read
        </Button>
      </Space>
      <List
        loading={status === 'loading'}
        locale={{ emptyText: 'No notifications yet.' }}
        dataSource={notifications}
        renderItem={(item) => (
          <List.Item
            style={{
              cursor: item.read ? 'default' : 'pointer',
              borderRadius: 10,
              marginBottom: 4,
              paddingInline: 10,
              background: item.read ? 'transparent' : 'rgba(21, 101, 192, 0.12)',
            }}
            onClick={() => {
              if (!item.read) {
                void dispatch(markNotificationRead(item.id));
              }
            }}
          >
            <List.Item.Meta
              title={
                <Space>
                  <Typography.Text>{item.title}</Typography.Text>
                  <Tag color={categoryColor[item.category] ?? 'default'}>{item.category}</Tag>
                </Space>
              }
              description={
                <Space direction="vertical" size={2}>
                  <Typography.Text type="secondary">{item.body}</Typography.Text>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {new Date(item.createdAt).toLocaleString()}
                  </Typography.Text>
                </Space>
              }
            />
          </List.Item>
        )}
      />
    </div>
  );

  return (
    <Popover
      content={content}
      trigger="click"
      open={panelOpen}
      onOpenChange={(nextOpen) => {
        dispatch(setPanelOpen(nextOpen));
      }}
      placement="bottomRight"
    >
      <Badge count={unreadCount} size="small">
        <Button icon={<BellOutlined />} shape="circle" />
      </Badge>
    </Popover>
  );
};
