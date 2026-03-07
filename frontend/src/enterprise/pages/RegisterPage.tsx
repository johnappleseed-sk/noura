import { FormEvent, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { Seo } from '@/components/common/Seo'
import { register } from '@/features/auth/authSlice'

/**
 * Renders the RegisterPage component.
 *
 * @returns The rendered component tree.
 */
export const RegisterPage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const status = useAppSelector((state) => state.auth.status)
  const error = useAppSelector((state) => state.auth.error)
  const isAuthenticated = useAppSelector((state) => state.auth.status === 'authenticated')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  if (isAuthenticated) {
    return <Navigate replace to="/" />
  }

  /**
   * Executes submit register.
   *
   * @param event The event value.
   * @returns No value.
   */
  const submitRegister = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault()
    const result = await dispatch(register({ fullName, email, password }))
    if (register.fulfilled.match(result)) {
      navigate('/')
    }
  }

  return (
    <div className="mx-auto max-w-lg">
      <Seo description="Create an account to track orders and personalize your shopping experience." title="Register" />
      <section className="panel space-y-4 p-6">
        <h1 className="m3-title">Create Account</h1>

        <form className="space-y-3" onSubmit={submitRegister}>
          <input
            className="m3-input"
            onChange={(event) => setFullName(event.target.value)}
            placeholder="Full name"
            required
            type="text"
            value={fullName}
          />
          <input
            className="m3-input"
            onChange={(event) => setEmail(event.target.value)}
            placeholder="Email"
            required
            type="email"
            value={email}
          />
          <input
            className="m3-input"
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Password"
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
            {status === 'loading' ? 'Creating account...' : 'Register'}
          </button>
        </form>

        <p className="m3-subtitle text-sm">
          Already have an account?{' '}
          <Link className="m3-link" to="/login">
            Login
          </Link>
        </p>
      </section>
    </div>
  )
}
