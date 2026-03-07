import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAdminDispatch, useAdminSelector } from '@/admin/app/hooks'
import { login, logout } from '@/admin/features/auth/authSlice'

interface LoginFormValues {
  email: string
  password: string
}

/**
 * Renders the LoginPage component.
 *
 * @returns The rendered component tree.
 */
export const LoginPage = (): JSX.Element => {
  const dispatch = useAdminDispatch()
  const navigate = useNavigate()
  const { token, user, loading, error } = useAdminSelector((state) => state.auth)

  useEffect(() => {
    if (token && user?.roles.includes('ADMIN')) {
      navigate('/dashboard', { replace: true })
      return
    }
    if (token && user && !user.roles.includes('ADMIN')) {
      dispatch(logout())
    }
  }, [dispatch, navigate, token, user])

  /**
   * Executes handle submit.
   *
   * @param values The values value.
   * @returns No value.
   */
  const handleSubmit = async (values: LoginFormValues): Promise<void> => {
    await dispatch(login(values))
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        background:
          'radial-gradient(circle at 20% 20%, rgba(22,119,255,0.2), transparent 45%), radial-gradient(circle at 80% 10%, rgba(19,194,194,0.22), transparent 40%), linear-gradient(135deg, #f7f8fa 0%, #eef2f7 100%)',
        padding: 20,
      }}
    >
      <Card style={{ width: 420 }} title="Admin Sign In">
        <Typography.Paragraph type="secondary">
          Use your admin credentials. Seed account: <code>admin@noura.com / password</code>
        </Typography.Paragraph>
        {error ? (
          <Alert
            showIcon
            message="Login failed"
            style={{ marginBottom: 16 }}
            type="error"
            description={error}
          />
        ) : null}
        <Form<LoginFormValues> layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="Email" name="email" rules={[{ required: true, message: 'Email is required' }]}>
            <Input placeholder="admin@noura.com" size="large" />
          </Form.Item>
          <Form.Item
            label="Password"
            name="password"
            rules={[{ required: true, message: 'Password is required' }]}
          >
            <Input.Password placeholder="••••••••" size="large" />
          </Form.Item>
          <Button block htmlType="submit" loading={loading} size="large" type="primary">
            Sign In
          </Button>
        </Form>
      </Card>
    </div>
  )
}
