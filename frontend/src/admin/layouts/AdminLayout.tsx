import {
  AppstoreOutlined,
  BarChartOutlined,
  BellOutlined,
  CheckCircleOutlined,
  DashboardOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MoonOutlined,
  ShopOutlined,
  ShoppingCartOutlined,
  SunOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Avatar, Button, Layout, Menu, Space, Switch, Tooltip, Typography } from 'antd';
import { useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { NotificationCenter } from '@/components/NotificationCenter';
import { logout } from '@/features/auth/authSlice';
import { toggleTheme } from '@/features/ui/themeSlice';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
  { key: '/analytics', icon: <BarChartOutlined />, label: 'Analytics' },
  { key: '/products', icon: <AppstoreOutlined />, label: 'Products' },
  { key: '/orders', icon: <ShoppingCartOutlined />, label: 'Orders' },
  { key: '/stores', icon: <ShopOutlined />, label: 'Stores' },
  { key: '/users', icon: <TeamOutlined />, label: 'Users' },
  { key: '/approvals', icon: <CheckCircleOutlined />, label: 'B2B Approvals' },
  { key: '/notifications', icon: <BellOutlined />, label: 'Notifications' },
];

const routeMeta: Record<string, { title: string; subtitle: string }> = {
  '/dashboard': {
    title: 'Operations Overview',
    subtitle: 'Track revenue, demand, and store health at a glance.',
  },
  '/analytics': {
    title: 'Advanced Analytics',
    subtitle: 'Explore trends, reports, and multidimensional business performance views.',
  },
  '/products': {
    title: 'Product Catalog',
    subtitle: 'Manage listings, pricing signals, and merchandising attributes.',
  },
  '/orders': {
    title: 'Order Operations',
    subtitle: 'Monitor fulfillment progress and update order lifecycle states.',
  },
  '/stores': {
    title: 'Store Directory',
    subtitle: 'Maintain locations, delivery settings, and service capabilities.',
  },
  '/users': {
    title: 'User Management',
    subtitle: 'Control role assignments, access states, and account governance.',
  },
  '/approvals': {
    title: 'B2B Approvals',
    subtitle: 'Review pending approvals and document decision rationale.',
  },
  '/notifications': {
    title: 'Notification Center',
    subtitle: 'Broadcast operational messages and targeted alerts in real time.',
  },
};

/**
 * Executes selected menu key.
 *
 * @param pathname The pathname value.
 * @returns The result of selected menu key.
 */
const selectedMenuKey = (pathname: string): string => {
  const found = menuItems.find((item) => pathname.startsWith(item.key));
  return found?.key ?? '/dashboard';
};

/**
 * Renders the AdminLayout component.
 *
 * @returns The rendered component tree.
 */
export const AdminLayout = (): JSX.Element => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const themeMode = useAppSelector((state) => state.theme.mode);
  const auth = useAppSelector((state) => state.auth);
  const [collapsed, setCollapsed] = useState(false);

  const selectedKey = useMemo(() => selectedMenuKey(location.pathname), [location.pathname]);
  const currentMeta = routeMeta[selectedKey] ?? routeMeta['/dashboard'];
  const userLabel = auth.fullName ?? auth.email ?? 'User';
  const userInitial = userLabel.trim().charAt(0).toUpperCase() || 'U';

  return (
    <Layout className="admin-shell">
      <Sider
        breakpoint="lg"
        collapsedWidth={80}
        width={250}
        className="admin-sider"
        collapsible
        trigger={null}
        collapsed={collapsed}
        onCollapse={(nextCollapsed) => setCollapsed(nextCollapsed)}
      >
        <div className="admin-brand">
          <div className="admin-brand-mark">NOURA</div>
          {!collapsed ? <div className="admin-brand-meta">Commerce Control Plane</div> : null}
        </div>
        <Menu
          className="admin-menu"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
        {!collapsed ? (
          <Button
            icon={<LogoutOutlined />}
            onClick={() => {
              dispatch(logout());
              navigate('/login');
            }}
          >
            Sign out
          </Button>
        ) : (
          <Tooltip title="Sign out" placement="right">
            <Button
              icon={<LogoutOutlined />}
              onClick={() => {
                dispatch(logout());
                navigate('/login');
              }}
            />
          </Tooltip>
        )}
      </Sider>
      <Layout>
        <Header className="admin-header">
          <div className="admin-header-left">
            <Space size={12} align="start">
              <Button
                icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={() => setCollapsed((current) => !current)}
              />
              <div>
                <Typography.Title level={1} className="admin-header-title">
                  {currentMeta.title}
                </Typography.Title>
                <Typography.Paragraph className="admin-header-subtitle">{currentMeta.subtitle}</Typography.Paragraph>
              </div>
            </Space>
          </div>

          <Space className="admin-header-actions">
            <NotificationCenter />
            <div className="admin-user-chip">
              <Avatar size="small" icon={<UserOutlined />}>
                {userInitial}
              </Avatar>
              <Typography.Text>{userLabel}</Typography.Text>
            </div>
            <Switch
              checkedChildren={<MoonOutlined />}
              unCheckedChildren={<SunOutlined />}
              checked={themeMode === 'dark'}
              onChange={() => dispatch(toggleTheme())}
              aria-label="Toggle color mode"
            />
          </Space>
        </Header>
        <Content className="admin-content">
          <div className="admin-page-container">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
};
