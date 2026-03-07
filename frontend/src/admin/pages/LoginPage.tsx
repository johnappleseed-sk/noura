import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, App as AntApp, Button, Card, Form, Input, Typography } from 'antd';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { login, selectIsAuthenticated } from '@/features/auth/authSlice';

interface LoginFormValues {
  email: string;
  password: string;
}

/**
 * Renders the LoginPage component.
 *
 * @returns The rendered component tree.
 */
export const LoginPage = (): JSX.Element => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { message } = AntApp.useApp();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const authState = useAppSelector((state) => state.auth);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  /**
   * Executes on finish.
   *
   * @param values The values value.
   * @returns No value.
   */
  const onFinish = async (values: LoginFormValues): Promise<void> => {
    const result = await dispatch(login(values));
    if (login.fulfilled.match(result)) {
      message.success('Welcome back');
      navigate('/dashboard', { replace: true });
    }
  };

  return (
    <div className="admin-login-shell">
      <Card className="admin-login-card">
        <span className="admin-login-eyebrow">Noura Enterprise</span>
        <Typography.Title level={1} className="admin-login-title">
          Control Center Sign In
        </Typography.Title>
        <Typography.Paragraph className="admin-login-subtitle">
          Authenticate with your `ADMIN` or `B2B` account to access operations, catalog, and workflow controls.
        </Typography.Paragraph>
        {authState.error ? (
          <Alert type="error" showIcon message="Authentication failed" description={authState.error} style={{ marginBottom: 16 }} />
        ) : null}
        <Form<LoginFormValues> layout="vertical" onFinish={onFinish} initialValues={{ email: '', password: '' }}>
          <Form.Item name="email" label="Email" rules={[{ required: true }, { type: 'email' }]}>
            <Input prefix={<UserOutlined />} placeholder="admin@enterprise.com" />
          </Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="Your password" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={authState.status === 'loading'}>
              Sign In
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};
