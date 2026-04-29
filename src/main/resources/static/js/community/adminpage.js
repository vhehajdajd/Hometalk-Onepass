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

    // 1. 카테고리 추가 로직
    if (addBtn) {
        addBtn.addEventListener('click', () => {
            const items = document.querySelectorAll('.category-item');
            if (items.length > 0) {
                // 첫 번째 아이템 복사
                const newItem = items[0].cloneNode(true);

                // 입력값 초기화 및 스타일 리셋
                const input = newItem.querySelector('input[name="categoryNames"]');
                input.value = '';
                input.classList.remove('error');

                // 삭제 버튼 활성화 및 기능 부여
                const removeBtn = newItem.querySelector('.btn-remove');
                removeBtn.disabled = false;
                removeBtn.onclick = function() {
                    newItem.remove();
                };

                container.appendChild(newItem);
                input.focus(); // 추가 후 바로 입력 가능하게 포커스
            }
        });
    }

    // 2. 폼 전송 전 유효성 검사
    document.getElementById('createBoardForm')
        .addEventListener('submit', (e) => {

            const inputs = document.querySelectorAll('input[name="categoryNames"]');
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
                e.preventDefault(); // 제출 막기
                alert("카테고리명을 모두 입력해주세요.");
            }
        });
});