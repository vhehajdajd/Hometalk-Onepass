import { useState } from 'react';

const BASE_URL = '/hometop/api/reservations';

const ReservationAction = ({ reservationId }) => {
    const [processing, setProcessing] = useState(false);

    const updateStatusCell = (status, reason = '') => {
        const statusCell = document.getElementById(`reservation-status-${reservationId}`);
        const actionCell = document.getElementById(`reservation-action-${reservationId}`);

        if (!statusCell || !actionCell) return;

        if (status === 'CONFIRMED') {
            statusCell.innerHTML = `
                <span class="status-badge status-active">
                    예약확정
                </span>
            `;
            actionCell.innerHTML = '';
        }

        if (status === 'REJECTED') {
            statusCell.innerHTML = `
                <span class="status-badge status-rejected">
                    반려됨
                </span>
            `;
            actionCell.innerHTML = `
                <div style="color: #d9534f; margin-top: 2px;">
                    <i class="fas fa-comment-dots"></i>
                    사유: <span>${reason}</span>
                </div>
            `;
        }
    };

    const approveReservation = () => {
        window.showConfirmModal('이 예약을 승인하시겠습니까?', async () => {
            try {
                setProcessing(true);

                const response = await window.apiFetch(`${BASE_URL}/${reservationId}/approve`, {
                    method: 'POST',
                });

                if (!response.ok) {
                    const errorMsg = await response.text();
                    window.showAlertModal(errorMsg || '승인 처리 중 오류가 발생했습니다.');
                    return;
                }

                updateStatusCell('CONFIRMED');
                window.showAlertModal('예약이 승인되었습니다.');
            } catch (error) {
                console.error(error);
                window.showAlertModal('서버 통신 중 오류가 발생했습니다.');
            } finally {
                setProcessing(false);
            }
        });
    };

    const rejectReservation = () => {
        openRejectReasonModal(async (reason) => {
            try {
                setProcessing(true);

                const response = await window.apiFetch(`${BASE_URL}/${reservationId}/cancel`, {
                    method: 'PATCH',
                    body: JSON.stringify({
                        reason,
                    }),
                });

                if (!response.ok) {
                    const errorMsg = await response.text();
                    window.showAlertModal(errorMsg || '반려 처리 중 오류가 발생했습니다.');
                    return;
                }

                updateStatusCell('REJECTED', reason);
                window.showAlertModal('예약이 반려되었습니다.');
            } catch (error) {
                console.error(error);
                window.showAlertModal('서버 통신 중 오류가 발생했습니다.');
            } finally {
                setProcessing(false);
            }
        });
    };

    return (
        <div className="flex-center gap-md">
            <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={approveReservation}
                disabled={processing}
            >
                승인
            </button>

            <button
                type="button"
                className="btn btn-point btn-sm"
                onClick={rejectReservation}
                disabled={processing}
            >
                반려
            </button>
        </div>
    );
};

function openRejectReasonModal(onSubmit) {
    document.getElementById('customRejectModal')?.remove();

    const modal = document.createElement('div');
    modal.id = 'customRejectModal';
    modal.innerHTML = `
        <div class="custom-modal-overlay">
            <div class="custom-modal-box" style="max-width: 420px;">
                <div class="custom-modal-message" style="text-align: left;">
                    <strong style="display:block; margin-bottom: 10px; color:#1f2937;">
                        예약 반려 사유 입력
                    </strong>

                    <textarea id="rejectReasonInput"
                              rows="5"
                              placeholder="반려 사유를 입력해주세요."
                              class="reject-reason-textarea"></textarea>
                </div>

                <div class="custom-modal-footer">
                    <button type="button" class="custom-modal-btn cancel-btn">
                        취소
                    </button>

                    <button type="button" class="custom-modal-btn confirm-btn">
                        반려 처리
                    </button>
                </div>
            </div>
        </div>
    `;

    document.body.appendChild(modal);

    const textarea = modal.querySelector('#rejectReasonInput');
    const cancelBtn = modal.querySelector('.cancel-btn');
    const confirmBtn = modal.querySelector('.confirm-btn');
    const overlay = modal.querySelector('.custom-modal-overlay');

    setTimeout(() => textarea.focus(), 50);

    function closeModal() {
        modal.remove();
        document.removeEventListener('keydown', escHandler);
    }

    function escHandler(e) {
        if (e.key === 'Escape') {
            closeModal();
        }
    }

    cancelBtn.addEventListener('click', closeModal);

    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
            closeModal();
        }
    });

    confirmBtn.addEventListener('click', () => {
        const reason = textarea.value.trim();

        if (!reason) {
            window.showAlertModal('반려 사유를 입력해야 합니다.');
            return;
        }

        closeModal();

        if (typeof onSubmit === 'function') {
            onSubmit(reason);
        }
    });

    document.addEventListener('keydown', escHandler);
}

export default ReservationAction;