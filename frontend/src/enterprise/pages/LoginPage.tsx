import { FormEvent, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { Seo } from '@/components/common/Seo'
import { login } from '@/features/auth/authSlice'

/**
 * Renders the LoginPage component.
 *
 * @returns The rendered component tree.
 */
export const LoginPage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const isAuthenticated = useAppSelector((state) => state.auth.status === 'authenticated')
  const status = useAppSelector((state) => state.auth.status)
  const error = useAppSelector((state) => state.auth.error)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  if (isAuthenticated) {
    const redirectTo = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'
    return <Navigate replace to={redirectTo} />
  }

  /**
   * Executes submit login.
   *
   * @param event The event value.
   * @returns No value.
   */
  const submitLogin = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()
    const result = await dispatch(login({ email, password }))
    if (login.fulfilled.match(result)) {
      navigate('/')
    }
  }

  return (
    <div className="mx-auto max-w-lg">
      <Seo description="Login to access checkout, order history, and profile management." title="Login" />
      <section className="panel space-y-4 p-6">
        <h1 className="m3-title">Login</h1>
        <p className="m3-subtitle text-sm">
          Sign in with your account credentials to access orders, checkout, and account tools.
        </p>

        <form className="space-y-3" onSubmit={submitLogin}>
          <input
            className="m3-input"
            onChange={(event) => setEmail(event.target.value)}
            required
            type="email"
            value={email}
          />
          <input
            className="m3-input"
            onChange={(event) => setPassword(event.target.value)}
            required
            type="password"
            value={password}
          />

          {error ? <p className="text-sm text-rose-600 dark:text-rose-300">{error}</p> : null}

          <button
            className="m3-btn m3-btn-filled w-full"
            disabled={status === 'loading'}
            type="submit"
          >
            {status === 'loading' ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <p className="m3-subtitle text-sm">
          New user?{' '}
          <Link className="m3-link" to="/register">
            Register here
          </Link>
        </p>
      </section>
    </div>
  )
}
