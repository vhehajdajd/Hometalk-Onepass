import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './features/auth/pages/LoginPage'
import MyPage from './features/auth/pages/MyPage'
import RegisterPage from './features/auth/pages/RegisterPage'
import HomePage from './features/dashboard/pages/HomePage'
import EntryPage from './features/parking/pages/EntryPage'
import ExitPage from './features/parking/pages/ExitPage'
import './App.css'

const CONTEXT_PATH = '/hometop'

function App() {
  return (
    <BrowserRouter basename={CONTEXT_PATH}>
      <Routes>
        <Route path="/auth" element={<LoginPage />} />
        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/register" element={<RegisterPage />} />
        <Route path="/myPage" element={<MyPage />} />

        <Route path="/home" element={<HomePage />} />
        <Route path="/parking/entry" element={<EntryPage />} />
        <Route path="/parking/exit" element={<ExitPage />} />

        <Route path="*" element={<Navigate to="/home" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
