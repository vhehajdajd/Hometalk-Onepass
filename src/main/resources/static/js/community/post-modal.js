/*
    showAlertModal, showConfirmModal
*/

function removeExistingModal() {
    document.getElementById('customAlertModal')?.remove();
    document.getElementById('customConfirmModal')?.remove();
}

function showAlertModal(message) {
    removeExistingModal();
    const modal = document.createElement('div');
    modal.id = 'customAlertModal';
    modal.innerHTML = `
        <div class="custom-modal-overlay">
            <div class="custom-modal-box">
                <div class="custom-modal-message">
                    ${message}
                </div>

                <div class="custom-modal-footer">
                    <button type="button" class="custom-modal-btn confirm-btn">
                        확인
                    </button>
                </div>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    const confirmBtn = modal.querySelector('.confirm-btn');
    const overlay = modal.querySelector('.custom-modal-overlay');

    function escHandler(e) {
        if (e.key === 'Escape') {
            closeModal();
        }
    }
    function closeModal() {
        modal.remove();
        document.removeEventListener('keydown', escHandler);
    }
    confirmBtn.addEventListener('click', closeModal);
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
            closeModal();
        }
    });
    document.addEventListener('keydown', escHandler);
}

function showConfirmModal(message, onConfirm) {
    removeExistingModal();
    const modal = document.createElement('div');
    modal.id = 'customConfirmModal';
    modal.innerHTML = `
        <div class="custom-modal-overlay">
            <div class="custom-modal-box">
                <div class="custom-modal-message">
                    ${message}
                </div>

                <div class="custom-modal-footer">
                    <button type="button" class="custom-modal-btn cancel-btn">
                        취소
                    </button>

                    <button type="button" class="custom-modal-btn confirm-btn">
                        확인
                    </button>
                </div>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    const cancelBtn = modal.querySelector('.cancel-btn');
    const confirmBtn = modal.querySelector('.confirm-btn');
    const overlay = modal.querySelector('.custom-modal-overlay');

    function escHandler(e) {
        if (e.key === 'Escape') {
            closeModal();
        }
    }

    function closeModal() {
        modal.remove();
        document.removeEventListener('keydown', escHandler);
    }

    cancelBtn.addEventListener('click', closeModal);
    confirmBtn.addEventListener('click', () => {

        closeModal();

        if (typeof onConfirm === 'function') {
            onConfirm();
        }
    });

    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
            closeModal();
        }
    });

    document.addEventListener('keydown', escHandler);
}
window.showAlertModal = showAlertModal;
window.showConfirmModal = showConfirmModal;
/* 중복 모달 방지, ESC 메모리 누수 방지, overlay 닫기, 공통 close 처리 */