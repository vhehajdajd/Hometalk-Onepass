import { useState } from 'react'
import { Link } from 'react-router-dom'

const CONTEXT_PATH = '/hometop'
const API_BASE_URL = '/api'

function LoginPage() {
  const [form, setForm] = useState({
    loginId: '',
    password: '',
    rememberMe: false,
  })
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (event) => {
    const { name, value, checked, type } = event.target
    setForm((current) => ({
      ...current,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setIsSubmitting(true)

    try {
      const body = new URLSearchParams({
        loginId: form.loginId,
        password: form.password,
      })

      if (form.rememberMe) {
        body.set('remember-me', 'on')
      }

      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        },
        credentials: 'include',
        body,
      })

      if (!response.ok) {
        throw new Error('아이디 또는 비밀번호가 일치하지 않습니다.')
      }

      const data = await response.json()
      window.location.href = `${CONTEXT_PATH}${data.redirectUrl || '/dashboard'}`
    } catch (error) {
      setErrorMessage(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-brand">
          <div>
            <p className="brand-name">HomeTalk OnePass</p>
            <h1 id="login-title">로그인</h1>
          </div>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label className="login-field">
            <span>아이디</span>
            <input
              name="loginId"
              value={form.loginId}
              onChange={updateField}
              autoComplete="username"
              placeholder="아이디를 입력하세요"
              required
            />
          </label>

          <label className="login-field">
            <span>비밀번호</span>
            <input
              name="password"
              type="password"
              value={form.password}
              onChange={updateField}
              autoComplete="current-password"
              placeholder="비밀번호를 입력하세요"
              required
            />
          </label>

          <label className="remember-row">
            <input
              name="rememberMe"
              type="checkbox"
              checked={form.rememberMe}
              onChange={updateField}
            />
            <span>자동 로그인</span>
          </label>

          {errorMessage ? <p className="login-message">{errorMessage}</p> : null}

          <button className="login-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <p className="signup-line">
          계정이 없으신가요? <Link to="/auth/register">회원가입</Link>
        </p>
      </section>
    </main>
  )
}

export default LoginPage
