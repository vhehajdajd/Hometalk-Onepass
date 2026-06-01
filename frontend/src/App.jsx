import LoginPage from './features/auth/pages/LoginPage'
import RegisterPage from './features/auth/pages/RegisterPage'
import './App.css'

const CONTEXT_PATH = '/hometop'


function App() {
  // 아직 React Router를 붙이지 않은 상태라 pathname으로 최소 라우팅만 처리한다.
  // Spring context-path가 /hometop이므로 프론트 라우트도 같은 prefix를 기준으로 비교한다.
  if (window.location.pathname === `${CONTEXT_PATH}/auth`) {
    return <LoginPage />
  }

  if (window.location.pathname === `${CONTEXT_PATH}/auth/register`) {
    return <RegisterPage />
  }

  return (
    <>
    </>
  )
}

export default App
