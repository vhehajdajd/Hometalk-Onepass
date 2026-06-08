import { useNavigate } from 'react-router-dom'
import './layout.css'

const CONTEXT_PATH = '/hometop'

function Sidebar({ currentPage, role }) {

  const handleLogout = async () => {
    await fetch(`${CONTEXT_PATH}/api/auth/logout`, {
      method: 'POST',
      credentials: 'include'
    })
    window.location.href = `${CONTEXT_PATH}/auth`
  }

  return (
    <aside className="sidebar">
      {/* 로고 */}
      <a href={`${CONTEXT_PATH}/home`} className="sidebar-logo">
        <svg width="90" height="64" viewBox="0 0 90 64">
          <circle cx="22" cy="8" r="1.5" fill="none" stroke="rgba(255,255,255,0.9)" strokeWidth="1.2"/>
          <circle cx="45" cy="4" r="1.5" fill="none" stroke="rgba(255,255,255,0.9)" strokeWidth="1.2"/>
          <circle cx="68" cy="8" r="1.5" fill="none" stroke="rgba(255,255,255,0.9)" strokeWidth="1.2"/>
          <path d="M22,10 Q30,19 36,16 Q40,14 45,6 Q50,14 54,16 Q60,19 68,10 L68,19 M22,10 L22,19"
                fill="none" stroke="rgba(255,255,255,0.9)" strokeWidth="1.2" strokeLinecap="round"/>
          <text x="45" y="38" textAnchor="middle" fill="white" fontSize="14" letterSpacing="4" fontWeight="300">HomeTalk</text>
          <line x1="10" y1="45" x2="80" y2="45" stroke="rgba(255,255,255,0.4)" strokeWidth="0.5"/>
          <text x="45" y="60" textAnchor="middle" fill="white" fontSize="15" letterSpacing="3" fontWeight="700">OnePass</text>
        </svg>
      </a>

      {/* 비로그인 */}
      {!role && (
        <nav className="sidebar-menu">
          <a className="sidebar-item" href={`${CONTEXT_PATH}/auth`}>로그인</a>
          <a className="sidebar-item" href={`${CONTEXT_PATH}/auth/register`}>회원가입</a>
        </nav>
      )}

      {/* 입주민 */}
      {role === 'RESIDENT' && (
        <nav className="sidebar-menu">
          <a className={`sidebar-item${currentPage === 'notice' ? ' active' : ''}`} href={`${CONTEXT_PATH}/notice`}>공지사항</a>
          <a className={`sidebar-item${currentPage === 'schedule' ? ' active' : ''}`} href={`${CONTEXT_PATH}/schedule`}>일정</a>
          <a className={`sidebar-item${currentPage === 'inquiry' ? ' active' : ''}`} href={`${CONTEXT_PATH}/inquiries/list`}>입주민지원</a>
          <a className={`sidebar-item${currentPage === 'reservation' ? ' active' : ''}`} href={`${CONTEXT_PATH}/reservation/calendar`}>시설 예약</a>
          <a className={`sidebar-item${currentPage === 'parking' ? ' active' : ''}`} href={`${CONTEXT_PATH}/parking/vehicle`}>주차</a>
          <a className={`sidebar-item${currentPage === 'billing' ? ' active' : ''}`} href={`${CONTEXT_PATH}/billing`}>관리비</a>
          <a className={`sidebar-item${currentPage === 'community' ? ' active' : ''}`} href={`${CONTEXT_PATH}/community/square/all`}>커뮤니티</a>
          <a className={`sidebar-item${currentPage === 'myPage' ? ' active' : ''}`} href={`${CONTEXT_PATH}/myPage`}>마이페이지</a>
        </nav>
      )}

      {/* 관리자 */}
      {role === 'ADMIN' && (
        <nav className="sidebar-menu">
          <a className={`sidebar-item${currentPage === 'notice' ? ' active' : ''}`} href={`${CONTEXT_PATH}/notice`}>공지사항</a>
          <a className={`sidebar-item${currentPage === 'schedule' ? ' active' : ''}`} href={`${CONTEXT_PATH}/schedule`}>일정</a>
          <a className={`sidebar-item${currentPage === 'inquiry' ? ' active' : ''}`} href={`${CONTEXT_PATH}/inquiries/list`}>입주민지원</a>
          <a className={`sidebar-item${currentPage === 'reservation' ? ' active' : ''}`} href={`${CONTEXT_PATH}/reservation/calendar`}>시설 예약</a>
          <a className={`sidebar-item${currentPage === 'parking' ? ' active' : ''}`} href={`${CONTEXT_PATH}/admin/vehicle/approval`}>주차</a>
          <a className={`sidebar-item${currentPage === 'billing' ? ' active' : ''}`} href={`${CONTEXT_PATH}/billing/admin/unpaid`}>관리비</a>
          <a className={`sidebar-item${currentPage === 'community' ? ' active' : ''}`} href={`${CONTEXT_PATH}/community/square/all`}>커뮤니티</a>
          <a className={`sidebar-item${currentPage === 'userApproval' ? ' active' : ''}`} href={`${CONTEXT_PATH}/admin/users/approvals`}>회원 승인</a>
        </nav>
      )}

      {/* 경비 */}
        {role === 'STAFF' && (
        <nav className="sidebar-menu">
            <a className={`sidebar-item${currentPage === 'parking' ? ' active' : ''}`} href="/hometop/parking/entry">입차</a>
            <a className={`sidebar-item${currentPage === 'manualEntry' ? ' active' : ''}`} href="/hometop/staff/manual-entry">수동 입차</a>
            <a className={`sidebar-item${currentPage === 'exit' ? ' active' : ''}`} href="/hometop/parking/exit">출차</a>
        </nav>
        )}

      {/* 하단 */}
      <div className="s-bottom">
        <div className="s-phone">
          <div className="s-phone-label">관리사무소</div>
          <div className="s-phone-num">02-3456-7890</div>
        </div>
        {role && (
          <button className="s-logout" onClick={handleLogout}>로그아웃</button>
        )}
      </div>
    </aside>
  )
}

export default Sidebar