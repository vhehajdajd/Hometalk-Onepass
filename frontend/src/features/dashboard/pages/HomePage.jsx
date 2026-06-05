import { useState, useEffect } from 'react'
import '../styles/home.css'
import bannerImg from '../../../assets/banner.png'
import Sidebar from '../../../components/Sidebar'
import Navbar from '../../../components/Navbar'
import Footer from '../../../components/Footer'

const CONTEXT_PATH = '/hometop'

function HomePage() {
  const [role, setRole] = useState(null)

  useEffect(() => {
    fetch(`${CONTEXT_PATH}/api/auth/mypage`, { credentials: 'include' })
      .then(res => {
        if (res.ok) return res.json()
        throw new Error()
      })
      .then(data => setRole(data.role))
      .catch(() => setRole(null))
  }, [])

  const goService = (serviceKey) => {
    if (!role) {
      alert('로그인이 필요한 서비스입니다.')
      window.location.href = `${CONTEXT_PATH}/auth`
      return
    }
    window.location.href = `${CONTEXT_PATH}/service/${serviceKey}`
  }

  return (
    <div className="main-layout" style={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar currentPage="home" role={role} />
      <div className="main-wrapper" style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
        <Navbar currentPage="home" />

        {/* 배너 */}
        <section className="banner">
          <img src={bannerImg} alt="포레스트 리움 단지 전경" />
          <div className="banner-overlay">
            <div className="banner-text">
              <h1>안녕하세요,<br />HomeTalk OnePass입니다.</h1>
              <p>포레스트 리움 단지 내 소식과 정보를<br />편리하게 이용하세요.</p>
            </div>
          </div>
        </section>

        {/* 정보 카드 */}
        <div className="container">
          <div className="info-grid">

            {/* 공지사항 - 클릭 안되게 */}
            <div className="card" style={{ opacity: 0.5, cursor: 'not-allowed' }}>
              <div className="card-header">
                <span className="card-header-left">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                  </svg>
                  공지사항
                </span>
              </div>
              <ul className="notice-list">
                <li className="notice-empty"></li>
              </ul>
            </div>

            {/* 오늘의 일정 - 클릭 안되게 */}
            <div className="card" style={{ opacity: 0.5, cursor: 'not-allowed' }}>
              <div className="card-header">
                <span className="card-header-left">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                  </svg>
                  오늘의 일정
                </span>
              </div>
              <ul className="schedule-list">
                <li className="schedule-empty"></li>
              </ul>
            </div>

            {/* 커뮤니티 */}
            <div className="card" style={{ cursor: 'pointer' }}>
              <div className="card-header">
                <span className="card-header-left">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                  </svg>
                  커뮤니티
                </span>
                <button className="more-btn" onClick={() => goService('community')}>
                  더보기
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="9 18 15 12 9 6"/>
                  </svg>
                </button>
              </div>
              <ul className="community-list">
                <li className="community-empty">최신 게시글이 없습니다.</li>
              </ul>
            </div>

          </div>

          {/* 주요 서비스 */}
          <p className="section-title">주요 서비스</p>
          <div className="service-grid">

            <div className="service-card" onClick={() => goService('civil')} style={{ cursor: 'pointer' }}>
              <div className="service-icon icon-civil">
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
                  <rect x="9" y="3" width="6" height="4" rx="2"/>
                  <line x1="9" y1="12" x2="15" y2="12"/>
                  <line x1="9" y1="16" x2="12" y2="16"/>
                </svg>
              </div>
              <span className="service-name">입주민지원</span>
              <span className="service-desc">민원 신청 및 처리 현황을<br />확인하세요.</span>
            </div>

            <div className="service-card" onClick={() => goService('facility')} style={{ cursor: 'pointer' }}>
              <div className="service-icon icon-facility">
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M2 20h20"/>
                  <path d="M4 20V8l8-6 8 6v12"/>
                  <path d="M9 20v-5h6v5"/>
                  <circle cx="12" cy="11" r="1"/>
                </svg>
              </div>
              <span className="service-name">시설예약</span>
              <span className="service-desc">단지 내 다양한 시설을<br />예약할 수 있어요.</span>
            </div>

            <div className="service-card" style={{ opacity: 0.5, cursor: 'not-allowed' }}>
              <div className="service-icon icon-billing">
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="2" y="5" width="20" height="14" rx="2"/>
                  <line x1="2" y1="10" x2="22" y2="10"/>
                  <line x1="7" y1="15" x2="7.01" y2="15"/>
                  <line x1="11" y1="15" x2="13" y2="15"/>
                </svg>
              </div>
              <span className="service-name">관리비 조회</span>
              <span className="service-desc">관리비 내역을<br />간편하게 조회하세요.</span>
            </div>

            <div className="service-card" onClick={() => goService('parking')} style={{ cursor: 'pointer' }}>
              <div className="service-icon icon-parking">
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="1" y="3" width="15" height="13" rx="2"/>
                  <path d="M16 8h4l3 3v5h-7V8z"/>
                  <circle cx="5.5" cy="18.5" r="2.5"/>
                  <circle cx="18.5" cy="18.5" r="2.5"/>
                </svg>
              </div>
              <span className="service-name">주차</span>
              <span className="service-desc">주차 등록 및 방문 차량<br />이용을 확인하세요.</span>
            </div>

            <div className="service-card" onClick={() => goService('community')} style={{ cursor: 'pointer' }}>
              <div className="service-icon icon-community">
                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
              </div>
              <span className="service-name">커뮤니티</span>
              <span className="service-desc">이웃과 소통하고<br />정보를 나눠요.</span>
            </div>

          </div>
        </div>

        <Footer />
      </div>
    </div>
  )
}

export default HomePage