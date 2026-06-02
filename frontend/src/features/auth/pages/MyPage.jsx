import { useEffect, useState } from 'react'

const CONTEXT_PATH = '/hometop'

const valueOrDash = (value) => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  return value
}

function MyPage() {
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true)
        setError('')

        const response = await fetch(`${CONTEXT_PATH}/api/auth/mypage`, {
          method: 'GET',
          credentials: 'include',
        })

        if (!response.ok) {
          throw new Error('회원 정보를 불러올 수 없습니다.')
        }

        const data = await response.json()
        setProfile(data)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [])

  const handleLogout = async () => {
    try {
      setIsLoggingOut(true)
      setError('')

      const response = await fetch(`${CONTEXT_PATH}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include',
      })

      if (!response.ok) {
        throw new Error('로그아웃을 처리할 수 없습니다.')
      }

      const data = await response.json()
      window.location.href = `${CONTEXT_PATH}${data.redirectUrl || '/auth?logout=true'}`
    } catch (err) {
      setError(err.message)
      setIsLoggingOut(false)
    }
  }

  if (loading) {
    return (
      <main className="my-page">
        <section className="my-page-state">로딩 중...</section>
      </main>
    )
  }

  const householdUnit = `${valueOrDash(profile?.dong)} / ${valueOrDash(profile?.ho)}`

  return (
    <main className="my-page">
      <section className="my-page-module" aria-labelledby="my-page-title">
        <div className="my-page-head">
          <div>
            <h1 id="my-page-title">마이페이지</h1>
            <p>내 계정 정보와 세대 정보를 확인합니다.</p>
          </div>
          <span className="my-login-type">{valueOrDash(profile?.authType)}</span>
        </div>

        {error ? <p className="my-page-message">{error}</p> : null}

        <div className="my-summary-grid">
          <article className="my-summary-card">
            <span>이름</span>
            <strong>{valueOrDash(profile?.name)}</strong>
          </article>
          <article className="my-summary-card">
            <span>닉네임</span>
            <strong>{valueOrDash(profile?.nickname)}</strong>
          </article>
          <article className="my-summary-card">
            <span>이메일</span>
            <strong>{valueOrDash(profile?.email)}</strong>
          </article>
          <article className="my-summary-card">
            <span>전화번호</span>
            <strong>{valueOrDash(profile?.phoneNumber)}</strong>
          </article>
        </div>

        <div className="my-panel-grid">
          <section className="my-panel">
            <h2>기본 정보</h2>
            <dl className="my-info-list">
              <div>
                <dt>소셜 플랫폼</dt>
                <dd>{valueOrDash(profile?.socialPlatform)}</dd>
              </div>
              <div>
                <dt>로그인 방식</dt>
                <dd>{valueOrDash(profile?.authType)}</dd>
              </div>
            </dl>
          </section>

          <section className="my-panel">
            <h2>세대 정보</h2>
            <dl className="my-info-list">
              <div>
                <dt>주소</dt>
                <dd>{valueOrDash(profile?.buildingName)}</dd>
              </div>
              <div>
                <dt>동 / 호</dt>
                <dd>{householdUnit}</dd>
              </div>
              <div>
                <dt>우편번호</dt>
                <dd>{valueOrDash(profile?.postNum)}</dd>
              </div>
            </dl>
          </section>
        </div>

        <section className="my-panel my-account-panel">
          <div>
            <h2>계정 관리</h2>
            <p>현재 계정에서 로그아웃할 수 있습니다.</p>
          </div>
          <div className="my-page-actions">
            <button
              className="my-action-btn my-action-logout"
              type="button"
              onClick={handleLogout}
              disabled={isLoggingOut}
            >
              {isLoggingOut ? '처리 중...' : '로그아웃'}
            </button>
          </div>
        </section>
      </section>
    </main>
  )
}

export default MyPage
