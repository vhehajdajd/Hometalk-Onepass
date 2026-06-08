import { useState, useEffect } from 'react'
import '../styles/parking-common.css'
import '../styles/parking-staff.css'
import Sidebar from '../../../components/Sidebar'
import Navbar from '../../../components/Navbar'
import Footer from '../../../components/Footer'


function EntryPage() {
  const [keyword, setKeyword] = useState('')
  const [searchError, setSearchError] = useState('')
  const [searchResult, setSearchResult] = useState([])
  const [visitList, setVisitList] = useState([])
  const [residentList, setResidentList] = useState([])
  const [activeTab, setActiveTab] = useState('visit')
  const [isProcessing, setIsProcessing] = useState(false)

  useEffect(() => {
    loadTodayList()
  }, [])

  const searchVehicle = async () => {
    if (keyword.length !== 4) {
      setSearchError('차량 번호 4자리를 입력해주세요.')
      return
    }
    setSearchError('')

    try {
      const res = await fetch(`/api/staff/vehicle/search?keyword=${keyword}`)
      if (!res.ok) {
        const data = await res.json()
        throw new Error(data.message || '서버 오류')
      }
      const data = await res.json()
      setSearchResult(data)
    } catch (err) {
      setSearchError(err.message || '조회 중 오류가 발생했습니다.')
    }
  }

  const processEntry = async (id, type) => {
    if (isProcessing) return
    setIsProcessing(true)

    try {
      const res = await fetch('/api/staff/vehicle/entry', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id, type })
      })
      if (!res.ok) {
        const data = await res.json()
        throw new Error(data.message || '서버 오류')
      }
      alert('입차 처리가 완료되었습니다.')
      setKeyword('')
      setSearchResult([])
      loadTodayList()
    } catch (err) {
      alert(err.message || '입차 처리 중 오류가 발생했습니다.')
    } finally {
      setIsProcessing(false)
    }
  }

  const loadTodayList = async () => {
    try {
      const visitRes = await fetch('/api/staff/entry/list/visit')
      const visitData = await visitRes.json()
      setVisitList(visitData)

      const residentRes = await fetch('/api/staff/entry/list/resident')
      const residentData = await residentRes.json()
      setResidentList(residentData)
    } catch (err) {
      console.error(err)
    }
  }

  return (
    <div className="main-layout" style={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar currentPage="parking" role="STAFF" />
      <div className="main-wrapper" style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <Navbar currentPage="parking" />
        <main style={{ flex: 1 }}>
          <div className="container">

            {/* 입차 퀵서치 */}
            <div className="section">
              <h2 className="page-title">입차</h2>
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
                {searchResult.length === 0 ? null : (
                  searchResult.map((vehicle, idx) => (
                    <div key={idx} className="result-item">
                      {vehicle.type === 'RESERVATION' ? (
                        <>
                          <p>*결과 : [{vehicle.vehicleNumber}] {vehicle.household} {vehicle.purpose} 차량입니다.</p>
                          <button className="pk-btn pk-btn-primary" onClick={() => processEntry(vehicle.reservationId, 'RESERVATION')}>입차 처리</button>
                        </>
                      ) : (
                        <>
                          <p>*결과 : [{vehicle.vehicleNumber}] {vehicle.household} 입주자 차량입니다.</p>
                          <button className="pk-btn pk-btn-primary" onClick={() => processEntry(vehicle.vehicleId, 'RESIDENT')}>입차 처리</button>
                        </>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* 오늘 입차 예정 */}
            <div className="section" style={{ marginTop: '40px' }}>
              <h2 className="page-title">오늘 입차 예정 차량</h2>

              <div className="tab-group">
                <button className={`tab-btn ${activeTab === 'visit' ? 'active' : ''}`} onClick={() => setActiveTab('visit')}>방문 차량</button>
                <button className={`tab-btn ${activeTab === 'resident' ? 'active' : ''}`} onClick={() => setActiveTab('resident')}>입주자 차량</button>
              </div>

              {activeTab === 'visit' && (
                <table className="park-table">
                  <thead>
                    <tr>
                      <th>차량 번호</th><th>방문세대</th><th>방문목적</th><th>방문 예정 시각</th><th>관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visitList.length === 0 ? (
                      <tr><td colSpan={5} className="pk-empty">오늘 방문 예정 차량이 없습니다.</td></tr>
                    ) : (
                      visitList.map((v, idx) => (
                        <tr key={idx}>
                          <td>{v.vehicleNumber}</td>
                          <td>{v.household}</td>
                          <td>{v.purpose}</td>
                          <td>{v.reservedAt}</td>
                          <td>
                            <button className="pk-btn pk-btn-primary" onClick={() => processEntry(v.reservationId, 'RESERVATION')}>입차 처리</button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              )}

              {activeTab === 'resident' && (
                <table className="park-table">
                  <thead>
                    <tr>
                      <th>차량 번호</th><th>세대</th><th>등록자</th><th>차량 모델</th><th>관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {residentList.length === 0 ? (
                      <tr><td colSpan={5} className="pk-empty">등록된 입주자 차량이 없습니다.</td></tr>
                    ) : (
                      residentList.map((v, idx) => (
                        <tr key={idx}>
                          <td>{v.vehicleNumber}</td>
                          <td>{v.household}</td>
                          <td>{v.userName}</td>
                          <td>{v.model ?? '-'}</td>
                          <td>
                            <button className="pk-btn pk-btn-primary" onClick={() => processEntry(v.vehicleId, 'RESIDENT')}>입차 처리</button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              )}
            </div>

          </div>
        </main>
        <Footer />
      </div>
    </div>
  )
}

export default EntryPage