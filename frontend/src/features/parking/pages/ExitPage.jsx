import { useState, useEffect } from 'react'
import '../styles/parking-common.css'
import '../styles/parking-staff.css'

function ExitPage() {
  const [keyword, setKeyword] = useState('')
  const [searchError, setSearchError] = useState('')
  const [searchResult, setSearchResult] = useState([])
  const [visitList, setVisitList] = useState([])
  const [residentList, setResidentList] = useState([])
  const [recentExitList, setRecentExitList] = useState([])
  const [activeTab, setActiveTab] = useState('visit')
  const [isProcessing, setIsProcessing] = useState(false)

  useEffect(() => {
    loadParkingList()
  }, [])

  const searchVehicle = async () => {
    if (keyword.length !== 4) {
      setSearchError('차량 번호 4자리를 입력해주세요.')
      return
    }
    setSearchError('')

    try {
      const res = await fetch(`/staff/exit/search?keyword=${keyword}`)
      if (!res.ok) throw new Error('서버 오류')
      const data = await res.json()
      setSearchResult(data)
    } catch (err) {
      setSearchError('조회 중 오류가 발생했습니다.')
    }
  }

  const processExit = async (parkingId) => {
    if (isProcessing) return
    if (!confirm('출차 처리하시겠습니까?')) return
    setIsProcessing(true)

    try {
      const res = await fetch('/staff/exit/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ parkingId })
      })
      if (!res.ok) throw new Error('서버 오류')
      alert('출차 처리가 완료되었습니다.')
      setKeyword('')
      setSearchResult([])
      loadParkingList()
    } catch (err) {
      alert('출차 처리 중 오류가 발생했습니다.')
    } finally {
      setIsProcessing(false)
    }
  }

  const forceExit = async (parkingId) => {
    if (isProcessing) return
    if (!confirm('현장 결제 완료 후 강제 출차 처리하시겠습니까?')) return
    setIsProcessing(true)

    try {
      const res = await fetch('/staff/exit/force', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ parkingId })
      })
      if (!res.ok) throw new Error('서버 오류')
      alert('강제 출차 처리가 완료되었습니다.')
      setKeyword('')
      setSearchResult([])
      loadParkingList()
    } catch (err) {
      alert('강제 출차 처리 중 오류가 발생했습니다.')
    } finally {
      setIsProcessing(false)
    }
  }

  const sendNotification = async (parkingId) => {
    if (isProcessing) return
    setIsProcessing(true)

    try {
      const res = await fetch('/staff/exit/notify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ parkingId })
      })
      if (!res.ok) throw new Error('서버 오류')
      alert('알림이 발송되었습니다.')
    } catch (err) {
      alert('알림 발송 중 오류가 발생했습니다.')
    } finally {
      setIsProcessing(false)
    }
  }

  const cancelExit = async (parkingId) => {
    if (isProcessing) return
    if (!confirm('출차를 취소하시겠습니까?')) return
    setIsProcessing(true)

    try {
      const res = await fetch('/staff/exit/cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ parkingId })
      })
      if (!res.ok) throw new Error('서버 오류')
      alert('출차가 취소되었습니다.')
      loadRecentExitList()
      loadParkingList()
    } catch (err) {
      alert('출차 취소 중 오류가 발생했습니다.')
    } finally {
      setIsProcessing(false)
    }
  }

  const loadParkingList = async () => {
    try {
      const visitRes = await fetch('/staff/exit/list/visit')
      setVisitList(await visitRes.json())

      const residentRes = await fetch('/staff/exit/list/resident')
      setResidentList(await residentRes.json())
    } catch (err) {
      console.error(err)
    }
  }

  const loadRecentExitList = async () => {
    try {
      const res = await fetch('/staff/exit/list/recent')
      setRecentExitList(await res.json())
    } catch (err) {
      console.error(err)
    }
  }

  const handleTabChange = (tab) => {
    setActiveTab(tab)
    if (tab === 'recentExit') loadRecentExitList()
  }

  return (
    <div className="container">

      {/* 출차 퀵서치 */}
      <div className="section">
        <h2 className="page-title">출차</h2>
        <div className="form-group">
          <label>차량 번호</label>
          <div className="search-group">
            <input
              type="text"
              placeholder="차량 번호 4자리 입력"
              maxLength={4}
              value={keyword}
              onChange={(e) => {
                const val = e.target.value.replace(/\D/g, '')
                setKeyword(val)
                if (val.length === 4) searchVehicle()
              }}
              onKeyDown={(e) => e.key === 'Enter' && searchVehicle()}
            />
            <button type="button" onClick={searchVehicle}>조회</button>
          </div>
          {searchError && <span className="error-msg">{searchError}</span>}
        </div>

        {/* 검색 결과 */}
        <div>
          {searchResult.map((vehicle, idx) => (
            <div key={idx} className="result-item">
              <p>*결과 : {vehicle.household} {vehicle.purpose ?? '-'} 차량입니다.</p>
              <ul>
                <li>주차 시간 : {vehicle.parkingTime}</li>
                <li>등록 티켓 : {vehicle.ticketInfo}</li>
                <li>판정 : {vehicle.canExit
                  ? <span className="can-exit">[출차 가능]</span>
                  : <span className="cannot-exit">[출차 불가 - {vehicle.householdConfirmed ? '티켓 부족' : '세대 미확인'}]</span>
                }</li>
              </ul>
              {vehicle.canExit ? (
                <button className="pk-btn pk-btn-primary" onClick={() => processExit(vehicle.parkingId)}>출차 처리</button>
              ) : (
                <>
                  {vehicle.householdConfirmed && (
                    <button className="pk-btn pk-btn-secondary" onClick={() => sendNotification(vehicle.parkingId)}>알림 보내기</button>
                  )}
                  <button className="pk-btn pk-btn-point" onClick={() => forceExit(vehicle.parkingId)}>현장 결제 후 강제 출차</button>
                </>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* 주차 중인 차량 목록 */}
      <div className="section">
        <h2 className="page-title">주차 중인 차량 목록</h2>

        <div className="tab-group">
          <button className={`tab-btn ${activeTab === 'visit' ? 'active' : ''}`} onClick={() => handleTabChange('visit')}>방문 차량</button>
          <button className={`tab-btn ${activeTab === 'resident' ? 'active' : ''}`} onClick={() => handleTabChange('resident')}>입주자 차량</button>
          <button className={`tab-btn ${activeTab === 'recentExit' ? 'active' : ''}`} onClick={() => handleTabChange('recentExit')}>최근 출차 기록</button>
        </div>

        {/* 방문 차량 탭 */}
        {activeTab === 'visit' && (
          <table className="park-table exit-table">
            <thead>
              <tr>
                <th>차량 번호</th><th>방문세대</th><th>방문목적</th><th>입차 시간</th><th>상태</th><th>관리</th>
              </tr>
            </thead>
            <tbody>
              {visitList.length === 0 ? (
                <tr><td colSpan={6} className="pk-empty">주차 중인 방문 차량이 없습니다.</td></tr>
              ) : (
                visitList.map((v, idx) => (
                  <tr key={idx}>
                    <td>{v.vehicleNumber}</td>
                    <td>{v.household}</td>
                    <td>{v.purpose ?? '-'}</td>
                    <td>{v.entryTime}</td>
                    <td>{v.canExit ? <span className="can-exit">출차 가능</span> : <span className="cannot-exit">출차 불가</span>}</td>
                    <td>
                      {v.canExit ? (
                        <button className="pk-btn pk-btn-primary" onClick={() => processExit(v.parkingId)}>출차</button>
                      ) : (
                        <>
                          {v.householdConfirmed && <button className="pk-btn pk-btn-secondary" onClick={() => sendNotification(v.parkingId)}>알림 보내기</button>}
                          <button className="pk-btn pk-btn-point" onClick={() => forceExit(v.parkingId)}>강제 출차</button>
                        </>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}

        {/* 입주자 차량 탭 */}
        {activeTab === 'resident' && (
          <table className="park-table exit-table">
            <thead>
              <tr>
                <th>차량 번호</th><th>세대</th><th>등록자</th><th>입차 시간</th><th>상태</th><th>관리</th>
              </tr>
            </thead>
            <tbody>
              {residentList.length === 0 ? (
                <tr><td colSpan={6} className="pk-empty">주차 중인 입주자 차량이 없습니다.</td></tr>
              ) : (
                residentList.map((v, idx) => (
                  <tr key={idx}>
                    <td>{v.vehicleNumber}</td>
                    <td>{v.household}</td>
                    <td>{v.userName ?? '-'}</td>
                    <td>{v.entryTime}</td>
                    <td>{v.canExit ? <span className="can-exit">출차 가능</span> : <span className="cannot-exit">출차 불가</span>}</td>
                    <td>
                      {v.canExit ? (
                        <button className="pk-btn pk-btn-primary" onClick={() => processExit(v.parkingId)}>출차</button>
                      ) : (
                        <>
                          {v.householdConfirmed && <button className="pk-btn pk-btn-secondary" onClick={() => sendNotification(v.parkingId)}>알림 보내기</button>}
                          <button className="pk-btn pk-btn-point" onClick={() => forceExit(v.parkingId)}>강제 출차</button>
                        </>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}

        {/* 최근 출차 기록 탭 */}
        {activeTab === 'recentExit' && (
          <>
            <p style={{ marginBottom: '8px', fontSize: '13px' }}>*당일 출차 건에 한하여 취소할 수 있습니다.</p>
            <table className="park-table exit-table">
              <thead>
                <tr>
                  <th>차량 번호</th><th>세대</th><th>입차 시간</th><th>출차 시간</th><th>주차 시간</th><th>관리</th>
                </tr>
              </thead>
              <tbody>
                {recentExitList.length === 0 ? (
                  <tr><td colSpan={6} className="pk-empty">오늘 출차된 차량이 없습니다.</td></tr>
                ) : (
                  recentExitList.map((v, idx) => (
                    <tr key={idx}>
                      <td>{v.vehicleNumber}</td>
                      <td>{v.household}</td>
                      <td>{v.entryTime}</td>
                      <td>{v.exitTime ?? '-'}</td>
                      <td>{v.parkingTime}</td>
                      <td>
                        {v.canCancelExit
                          ? <button className="pk-btn pk-btn-secondary" onClick={() => cancelExit(v.parkingId)}>출차 취소</button>
                          : <span style={{ color: '#aaa' }}>취소 불가</span>
                        }
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </>
        )}
      </div>

    </div>
  )
}

export default ExitPage
