import { useEffect, useState } from 'react';

const BASE_URL = '/hometop/api/reservations';

const statusLabel = {
    PENDING: '승인대기',
    CONFIRMED: '예약확정',
    REJECTED: '반려됨',
    CANCELED: '취소',
    FINISHED: '이용종료',
};

const statusClass = {
    PENDING: 'status-pending',
    CONFIRMED: 'status-active',
    REJECTED: 'status-rejected',
    CANCELED: 'status-canceled',
    FINISHED: 'status-finished',
};

const formatDateTime = (value) => {
    if (!value) return '-';

    const date = new Date(value);
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hour = String(date.getHours()).padStart(2, '0');
    const minute = String(date.getMinutes()).padStart(2, '0');

    return `${month}/${day} ${hour}:${minute}`;
};

const ReservationAdminTable = () => {
    const [reservations, setReservations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedStatus, setSelectedStatus] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    useEffect(() => {
        fetchReservations();
    }, [page, setSelectedStatus]);

    const fetchReservations = async () => {
        try {
            setLoading(true);
            setError(null);

            const query = new URLSearchParams({page, size: 10});

            if (selectedStatus) query.append('status', selectedStatus);

            const response = await window.apiFetch(`${BASE_URL}?${query.toString()}`);

            if (!response.ok) {
                throw new Error(`예약 목록 조회 실패: ${response.status}`);
            }

            const data = await response.json();
            setReservations(data.content ?? []);
            setTotalPages(data.totalPages ?? 0);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const filteredReservations = selectedStatus
        ? reservations.filter((res) => res.status === selectedStatus)
        : reservations;

    if (loading) {
        return (
            <div className="text-muted" style={{ padding: '40px 0', textAlign: 'center' }}>
                예약 내역을 불러오는 중입니다...
            </div>
        );
    }

    if (error) {
        return (
            <div className="alert-box alert-danger">
                <div className="alert-content">
                    <i className="fas fa-exclamation-circle"></i>
                    <span>{error}</span>
                </div>
            </div>
        );
    }

    return (
        <>
            <div className="filter-section flex-between mb-md" style={{ alignItems: 'center' }}>
                <h3 className="m-0" style={{ fontSize: '1.1rem', color: '#444' }}>
                    상세 내역
                </h3>

                <select
                    className="form-select"
                    value={selectedStatus}
                    onChange={(e) => {
                        setSelectedStatus(e.target.value);
                        setPage(0);
                    }}
                >
                    <option value="">전체 상태 보기</option>
                    <option value="PENDING">승인대기</option>
                    <option value="CONFIRMED">예약확정</option>
                    <option value="REJECTED">반려됨</option>
                    <option value="CANCELED">취소</option>
                    <option value="FINISHED">이용종료</option>
                </select>
            </div>

            <div className="summary-card">
                <table className="table">
                    <thead>
                    <tr>
                        <th>시설명</th>
                        <th>예약자</th>
                        <th>예약 시간 / 신청일 / 취소일</th>
                        <th>상태</th>
                        <th>관리</th>
                    </tr>
                    </thead>

                    <tbody>
                    {filteredReservations.length === 0 ? (
                        <tr>
                            <td colSpan="5" className="text-muted" style={{ padding: '60px 0' }}>
                                현재 등록된 예약 내역이 없습니다.
                            </td>
                        </tr>
                    ) : (
                        filteredReservations.map((res) => (
                            <tr key={res.id}>
                                <td>{res.facilityName}</td>
                                <td>{res.userName}</td>
                                <td>
                                    <div>
                                        {formatDateTime(res.startTime)} ~{' '}
                                        {res.endTime
                                            ? String(new Date(res.endTime).getHours()).padStart(2, '0') +
                                            ':' +
                                            String(new Date(res.endTime).getMinutes()).padStart(2, '0')
                                            : '-'}
                                    </div>

                                    <div style={{ fontSize: '0.75rem', color: '#999' }}>
                                        {res.status === 'CANCELED' && res.updatedAt
                                            ? `취소일: ${formatDateTime(res.updatedAt)}`
                                            : res.createdAt
                                                ? `신청일: ${formatDateTime(res.createdAt)}`
                                                : ''}
                                    </div>
                                </td>
                                <td>
                                    <span className={`status-badge ${statusClass[res.status] ?? 'status-active'}`}>
                                        {statusLabel[res.status] ?? res.status}
                                    </span>
                                </td>
                                <td>
                                    {res.status === 'PENDING' ? (
                                        <div className="flex-center gap-md">
                                            <button type="button" className="btn btn-primary btn-sm">
                                                승인
                                            </button>
                                            <button type="button" className="btn btn-point btn-sm">
                                                반려
                                            </button>
                                        </div>
                                    ) : (
                                        res.status === 'REJECTED' &&
                                        res.cancelReason && (
                                            <div style={{ color: '#d9534f', marginTop: '2px' }}>
                                                <i className="fas fa-comment-dots"></i>{' '}
                                                사유: {res.cancelReason}
                                            </div>
                                        )
                                    )}
                                </td>
                            </tr>
                        ))
                    )}
                    </tbody>
                </table>

                {totalPages > 1 && (
                    <div className="pagination-container">
                        <nav className="pagination-nav">
                            <ul className="pagination">
                                {Array.from({ length: totalPages }, (_, index) => (
                                    <li
                                        key={index}
                                        className={`page-item ${page === index ? 'active' : ''}`}
                                    >
                                        <button
                                            type="button"
                                            className="page-link"
                                            onClick={() => setPage(index)}
                                        >
                                            {index + 1}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        </nav>
                    </div>
                )}
            </div>
        </>
    );
};

export default ReservationAdminTable;