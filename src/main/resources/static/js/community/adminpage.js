// 모달 제어
function openCreateModal() {
    document.getElementById('createModal').classList.add('show');
}

function closeModal() {
    const modal = document.getElementById('createModal');
    modal.classList.remove('show');
    document.getElementById('createBoardForm').reset();

    // 카테고리가 여러 개 추가되어 있었다면 첫 번째만 남기고 초기화
    const container = document.getElementById('category-container');
    const items = container.querySelectorAll('.category-item-wrapper');
    for (let i = 1; i < items.length; i++) {
        items[i].remove();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('category-container');
    const addBtn = document.getElementById('btn-add-category');
    const createBoardForm = document.getElementById('createBoardForm');

    // 2. 카테고리 추가 로직 (반반 프리셋 대응)
    if (addBtn && container) {
        addBtn.addEventListener('click', () => {
            const items = document.querySelectorAll('.category-item-wrapper');

            if (items.length >= 5) {
                alert("카테고리는 최대 5개까지만 생성 가능합니다.");
                return;
            }

            if (items.length > 0) {
                const newItem = items[0].cloneNode(true);
                const nextIdx = items.length; // 중복 방지를 위한 인덱스

                // 텍스트 입력값 초기화
                const nameInput = newItem.querySelector('input[name="categoryNames"]');
                const codeInput = newItem.querySelector('input[name="categoryCodes"]');
                if (nameInput) nameInput.value = '';
                if (codeInput) codeInput.value = '';

                // ★ 중요: 라디오 버튼 Group Name 변경 (서로 다른 카테고리가 섞이지 않게)
                const radios = newItem.querySelectorAll('input[type="radio"]');
                radios.forEach(radio => {
                    radio.name = 'categoryTheme' + nextIdx; // categoryTheme0, categoryTheme1...
                    // 첫 번째 프리셋(딥블루)이 기본 체크되도록
                    if (radio.value.includes("#003366")) radio.checked = true;
                });

                // 삭제 버튼 활성화
                const removeBtn = newItem.querySelector('.btn-remove');
                if (removeBtn) {
                    removeBtn.disabled = false;
                    removeBtn.onclick = function() {
                        newItem.remove();
                    };
                }

                container.appendChild(newItem);
                nameInput?.focus();
            }
        });
    }

    // 3. 폼 전송 전 유효성 검사 및 데이터 가공
    if (createBoardForm) {
        createBoardForm.addEventListener('submit', (e) => {
            const nameInputs = document.querySelectorAll('input[name="categoryNames"]');
            const codeInputs = document.querySelectorAll('input[name="categoryCodes"]');
            let isValid = true;

            // 필수 입력 체크
            nameInputs.forEach((input, index) => {
                if (!input.value.trim() || !codeInputs[index].value.trim()) {
                    input.classList.add('error');
                    codeInputs[index].classList.add('error');
                    isValid = false;
                } else {
                    input.classList.remove('error');
                    codeInputs[index].classList.remove('error');
                }
            });

            if (!isValid) {
                e.preventDefault();
                alert("모든 칸(이름 및 영문 코드)을 입력해주세요.");
                return;
            }

            createBoardForm.querySelectorAll('.temp-hidden-input').forEach(el => el.remove());

            // 각 카테고리 아이템별로 선택된 테마를 찾아 분리 저장
            const wrappers = document.querySelectorAll('.category-item-wrapper');
            wrappers.forEach((wrapper, index) => {
                const selectedTheme = wrapper.querySelector(`input[name="categoryTheme${index}"]:checked`);
                if (selectedTheme) {
                    const [bgColor, textColor] = selectedTheme.value.split('|');

                    // 배경색 hidden input 생성
                    const bgInput = document.createElement('input');
                    bgInput.type = 'hidden';
                    bgInput.name = 'categoryBgColors';
                    bgInput.value = bgColor;
                    bgInput.className = 'temp-hidden-input';
                    createBoardForm.appendChild(bgInput);

                    // 글자색 hidden input 생성
                    const textInput = document.createElement('input');
                    textInput.type = 'hidden';
                    textInput.name = 'categoryTextColors';
                    textInput.value = textColor;
                    textInput.className = 'temp-hidden-input';
                    createBoardForm.appendChild(textInput);
                }
            });
        });
    }
});