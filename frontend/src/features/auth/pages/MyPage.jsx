import { useState, useEffect } from 'react'

// 마이페이지/ 단순 데이터 출력
const CONTEXT_PATH = '/hometop'

function MyPage() {
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true)
        setError('')

        const res = await fetch(`${CONTEXT_PATH}/api/auth/MyPageApiController`, {
          method: 'GET',
          credentials: 'include',
        })

        if (!res.ok) {
          throw new Error(`회원 정보를 가져올 수 없습니다. 오류 내용: ${res.status}`)
        }

        const data = await res.json()
        setProfile(data)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [])

  if (loading) return <div className="MyPage">로딩 중...</div>
  if (error) return <div className="MyPage error">{error}</div>

  return (
    <main className="MyPage">
      <section className="MyPage-panel">
        <div></div>
        <h1>마이페이지</h1>
        {profile && (
          <div className="profile-info">
            <p>이름: {profile.name}</p>
            <p>별명: {profile.nickname}</p>
            <p>이메일: {profile.email}</p>
          </div>
        )}
      </section>
    </main>
  )
}

export default MyPage