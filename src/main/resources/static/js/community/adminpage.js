// 모달 열기
function openCreateModal() {
    document.getElementById('createModal').classList.add('show');
}

// 모달 닫기
function closeModal() {
    const modal = document.getElementById('createModal');
    modal.classList.remove('show');

    document.getElementById('createBoardForm').reset();
}

document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('category-container');
    const addBtn = document.getElementById('btn-add-category');
    const createBoardForm = document.getElementById('createBoardForm');

    // 1. 카테고리 추가 로직 (게시판 생성 페이지용)
    if (addBtn && container) {
        addBtn.addEventListener('click', () => {
            // 현재 존재하는 카테고리 아이템 중 첫 번째 것을 기준으로 복사
            const items = document.querySelectorAll('.category-item');
            if (items.length > 0) {
                const newItem = items[0].cloneNode(true);

                // 이름(Names)과 코드(Codes) input을 모두 찾아서 초기화
                const nameInput = newItem.querySelector('input[name="categoryNames"]');
                const codeInput = newItem.querySelector('input[name="categoryCodes"]');
                if (nameInput) {
                    nameInput.value = '';
                    nameInput.classList.remove('error');
                }
                if (codeInput) {
                    codeInput.value = '';
                    codeInput.classList.remove('error');
                }

                if (colorInput) {
                    colorInput.value = '#EB6E57';
                }

                // 삭제 버튼 활성화 및 기능 연결
                const removeBtn = newItem.querySelector('.btn-remove');
                if (removeBtn) {
                    removeBtn.disabled = false;
                    removeBtn.onclick = function() {
                        newItem.remove();
                    };
                }
                container.appendChild(newItem);
                // 새로 생긴 이름 칸에 바로 타이핑할 수 있게 포커스
                nameInput?.focus();
            }
        });
    }

    // 2. 폼 전송 전 유효성 검사 (게시판 생성 페이지용)
    // 요소가 있을 때만 addEventListener를 실행하도록 수정
    if (createBoardForm) {
        createBoardForm.addEventListener('submit', (e) => {
            const inputs = document.querySelectorAll('input[name="categoryNames"]');
            const codeInputs = document.querySelectorAll('input[name="categoryCodes"]');
            let isValid = true;

            inputs.forEach(input => {
                if (!input.value.trim()) {
                    input.classList.add('error');
                    isValid = false;
                } else {
                    input.classList.remove('error');
                }
            });

            if (!isValid) {
                e.preventDefault();
                alert("모든 칸(이름 및 영문 코드)을 입력해주세요.");
            }
        });
    }
});