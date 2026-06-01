import { useState } from 'react'
import { Link } from 'react-router-dom'

// Spring Boot의 server.servlet.context-path와 맞춘다.
// API 호출과 백엔드 페이지 이동은 항상 이 prefix를 붙여 요청한다.
const CONTEXT_PATH = '/hometop'

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

    try {
      // Spring Security formLogin은 JSON body가 아니라 form-urlencoded 파라미터를 읽는다.
      // 그래서 React에서도 기존 HTML form submit과 같은 형태로 loginId/password를 전송한다.
      const body = new URLSearchParams({
        loginId: form.loginId,
        password: form.password,
      })

      // Remember-me 필터가 기대하는 파라미터명은 Spring Security 설정의 rememberMeParameter와 맞춘다.
      if (form.rememberMe) {
        body.set('remember-me', 'on')
      }

      // 이 URL은 SecurityConfig의 loginProcessingUrl("/api/auth/login")에서 처리된다.
      const response = await fetch(`${CONTEXT_PATH}/api/auth/login`, {
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
      // 성공 handler가 redirectUrl을 JSON으로 내려주면 React가 직접 화면을 이동한다.
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

          <button className="login-submit" type="submit">로그인
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
