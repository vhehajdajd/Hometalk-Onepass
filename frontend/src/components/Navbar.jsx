const CONTEXT_PATH = '/hometop'

function Navbar({ currentPage }) {

  const getPageName = (page) => {
    const pages = {
      notice: '공지사항',
      schedule: '일정',
      inquiry: '문의',
      reservation: '예약',
      parking: '주차',
      billing: '관리비',
      community: '커뮤니티',
      myPage: '마이페이지',
      home: '홈',
      approvals: '회원 승인',
      notification: '알림',
    }
    return pages[currentPage] || '홈'
  }

  return (
    <div className="header">
      <span>{getPageName(currentPage)}</span>
      <div style={{ display: 'flex', alignItems: 'center', gap: '20px', marginLeft: 'auto' }}>
        <a href={`${CONTEXT_PATH}/myPage`} style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--color-main)', textDecoration: 'none', fontSize: '14px', fontWeight: '600' }}>
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="8" r="4"/>
            <path d="M20 21a8 8 0 1 0-16 0"/>
          </svg>
          MyPage
        </a>
      </div>
    </div>
  )
}

export default Navbar