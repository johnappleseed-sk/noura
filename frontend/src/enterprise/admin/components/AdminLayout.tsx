import { Button, Layout, Menu, Space, Switch, Typography } from 'antd'
import {
  BellOutlined,
  CheckSquareOutlined,
  DashboardOutlined,
  LogoutOutlined,
  OrderedListOutlined,
  ShopOutlined,
  ShoppingOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useMemo } from 'react'
import { useAdminDispatch, useAdminSelector } from '@/admin/app/hooks'
import { logout } from '@/admin/features/auth/authSlice'
import { toggleThemeMode } from '@/admin/features/theme/themeSlice'

const { Header, Sider, Content } = Layout
const { Title, Text } = Typography

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
  { key: '/products', icon: <ShoppingOutlined />, label: 'Products' },
  { key: '/orders', icon: <OrderedListOutlined />, label: 'Orders' },
  { key: '/stores', icon: <ShopOutlined />, label: 'Stores' },
  { key: '/users', icon: <TeamOutlined />, label: 'Users' },
  { key: '/approvals', icon: <CheckSquareOutlined />, label: 'B2B Approvals' },
  { key: '/notifications', icon: <BellOutlined />, label: 'Notifications' },
]

/**
 * Renders the AdminLayout component.
 *
 * @returns The rendered component tree.
 */
export const AdminLayout = (): JSX.Element => {
  const dispatch = useAdminDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAdminSelector((state) => state.auth.user)
  const themeMode = useAdminSelector((state) => state.theme.mode)

  const selectedKey = useMemo(() => {
    const current = menuItems.find((item) => location.pathname.startsWith(item.key))
    return current?.key ?? '/dashboard'
  }, [location.pathname])

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth={80} width={240}>
        <div style={{ color: '#fff', padding: '18px 16px' }}>
          <Title level={4} style={{ color: '#fff', margin: 0 }}>
            Noura Admin
          </Title>
          <Text style={{ color: 'rgba(255,255,255,0.75)' }}>Enterprise Console</Text>
        </div>
        <Menu
          items={menuItems}
          mode="inline"
          selectedKeys={[selectedKey]}
          theme="dark"
          onClick={({ key }) => navigate(String(key))}
        />
      </Sider>
      <Layout>
        <Header style={{ padding: '0 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space>
            <Text type="secondary">Signed in as</Text>
            <Text strong>{user?.fullName ?? 'Unknown'}</Text>
          </Space>
          <Space>
            <Text>{themeMode === 'dark' ? 'Dark' : 'Light'} Theme</Text>
            <Switch checked={themeMode === 'dark'} onChange={() => dispatch(toggleThemeMode())} />
            <Button icon={<LogoutOutlined />} onClick={() => dispatch(logout())}>
              Logout
            </Button>
          </Space>
        </Header>
        <Content style={{ margin: 20 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
