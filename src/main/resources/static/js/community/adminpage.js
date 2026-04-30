// 1. 카테고리 추가 버튼 클릭 시
document.getElementById('btn-add-category').addEventListener('click', () => {
    const container = document.getElementById('category-container');
    const newItem = document.querySelector('.category-item').cloneNode(true);

    // 입력값 초기화 및 삭제 버튼 활성화
    newItem.querySelectorAll('input').forEach(input => {
        input.value = '';
        input.classList.remove('error'); // 이전 에러 표시 제거
    });

    const removeBtn = newItem.querySelector('.btn-remove');
    removeBtn.disabled = false; // 추가된 칸은 삭제 가능
    removeBtn.addEventListener('click', () => newItem.remove());

    container.appendChild(newItem);
});

// 2. 저장 버튼 클릭 시 유효성 검사
document.getElementById('btn-submit').addEventListener('click', () => {
    let isValid = true;
    const categoryInputs = document.querySelectorAll('input[name="categoryNames"]');

    categoryInputs.forEach(input => {
        if (input.value.trim() === "") {
            input.classList.add('error'); // 코랄색 테두리 활성화
            isValid = false;
        } else {
            input.classList.remove('error');
        }
    });

    if (!isValid) {
        alert("카테고리명은 필수입니다.");
        return;
    }

    // 유효성 검사 통과 시 Fetch API로 서버에 데이터 전송
    // AdminBoardCreateRequestDTO 형태에 맞춰서 JSON 구성
});


function toggleCreateForm() {
    const form = document.getElementById('createFormArea');
    form.style.display = form.style.display === 'none' ? 'block' : 'none';
}