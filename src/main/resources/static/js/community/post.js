/* ================================================
    [1] 페이지 초기화 & 이벤트 바인딩
=================================================== */
/* 안전장치로 DOMContentLoaded로 묶음 */
document.addEventListener('DOMContentLoaded', () => {
    // 1. 제목 글자 수 카운트
    const titleInput = document.getElementById('title');
    if (titleInput) {
        titleInput.addEventListener('input', updateCharCount);
    }

    // 2. 임시저장 목록 모달 열기
    const btnLoadTemp = document.getElementById('btnLoadTemp');
    if (btnLoadTemp) {
        btnLoadTemp.addEventListener('click', function() {
            const boardCode = this.dataset.boardCode || 'default';
            loadTempList(boardCode);
        });
    }

    // 3. 모달 닫기
    const closeBtn = document.querySelector('.close-modal');
    if (closeBtn) {
        closeBtn.onclick = () => {
            document.getElementById('tempListModal').style.display = 'none';
        };
    }

    // 4. 삭제 버튼 (상세 페이지)
    const btnDelete = document.getElementById('btn-delete');
    if (btnDelete) {
        btnDelete.addEventListener('click', deletePost);
    }

    // 5. 임시저장 실행 버튼 (작성 페이지)
    const btnSaveTemp = document.getElementById('btnSaveTemp');
    if (btnSaveTemp) {
        btnSaveTemp.addEventListener('click', saveTemp);
    }
});
/* ================================================
    [2] 게시글 작성 & 임시저장 기능
=================================================== */
// 제목 글자 수 업데이트
function updateCharCount() {
    const charCount = document.getElementById('charCount');
    if (charCount) {
        charCount.innerText = this.value.length;
    }
}

// 임시저장 실행
function saveTemp() {
    const categoryElement = document.getElementById('categoryId');
    const categoryValue = categoryElement ? categoryElement.value : "";

    if (!categoryValue || categoryValue === "") {
        alert("카테고리를 선택해야 임시저장이 가능합니다.");
        if (categoryElement) categoryElement.focus();
        return;
    }

    if (confirm("현재 내용을 임시저장하시겠습니까?")) {
        document.getElementById('isTemp').value = "true";
        alert("임시저장 되었습니다.");
        document.getElementById('postForm').submit();
    }
}

// 임시저장 목록 호출
function loadTempList(boardCode) {
    fetch(`/hometalk/community/${boardCode}/temp-list`)
        .then(response => {
        if (!response.ok) throw new Error('목록을 불러오는데 실패했습니다.');
        return response.json();
    })
        .then(data => {
        const listArea = document.getElementById('tempListArea');
        listArea.innerHTML = '';

        if (!data || data.length === 0) {
            listArea.innerHTML = '<li class="no-data">임시저장된 글이 없습니다.</li>';
        } else {
            const html = data.map(post => `
                    <li onclick="location.href='/hometalk/community/${boardCode}/write?id=${post.id}'" style="cursor:pointer;">
                        <span class="temp-category">[${post.categoryName || '미지정'}]</span>
                        <span class="temp-title">${post.title || '제목 없음'}</span>
                        <span class="temp-date">${post.createdAt || ''}</span>
                    </li>
                `).join('');
            listArea.innerHTML = html;
        }
        document.getElementById('tempListModal').style.display = 'block';
    })
        .catch(err => {
        console.error(err);
        alert('임시저장 목록을 가져오는 중 오류가 발생했습니다.');
    });
}

/* ================================================
    [3] 게시글 상세 & 삭제 기능
=================================================== */
// 취소 버튼 컨펌
function confirmCancel() {
    return confirm("작성 중인 내용을 중단하고 목록으로 돌아가시겠습니까?\n(임시저장된 내용은 보존됩니다.)");
}

// 게시글 삭제 (Soft Delete)
function deletePost() {
    const postId = this.dataset.id;     // 버튼의 data-id 속성
    if (confirm("정말 삭제하시겠습니까? \n 삭제된 글을 목록에서 사라집니다.")) {
        const deleteForm = document.getElementById('deleteForm');
        if (deleteForm) {
            console.log(postId + "번 게시글 삭제 요청");
            deleteForm.submit();
        } else {
            console.error("삭제 폼(deleteForm)을 찾을 수 없습니다.")
        }
    }
}

// 상단 고정
function togglePin(postId) {
    // 1. 확인 메시지
    if (!confirm("이 게시글을 상단 고정하시겠습니까?")) return;

    // 2. 서버로 상태 변경 요청 (Fetch API 사용)
    fetch(`/api/posts/${postId}/pin`, {
            method: 'POST'
        })
        .then(response => {
            if (response.ok) {
                alert("상태가 변경되었습니다.");
                location.reload(); // 성공 시 화면 새로고침하여 바뀐 상태 반영
            } else {
                return response.json().then(err => { throw new Error(err.message); });
            }
        })
        .catch(err => {
            console.error(err);
            alert("고정 처리 중 오류가 발생했습니다: " + err.message);
        });
}

// 숨김 처리
function hidePost(postId) {
    if(!confirm("정말 숨기시겠습니까?")) return;

    const adminArea = document.querySelector('.admin-tools');
    const boardCode = adminArea.dataset.boardCode;
    const categoryCode = adminArea.dataset.categoryCode || 'all';

    fetch(`/api/posts/${postId}/hide`, {
        method: 'POST'
    })
        .then(response => {
        if (response.ok) {
            alert("숨김 처리가 완료되었습니다.");
            // 원래 있던 게시판 목록으로 이동
            location.href = `/community/${boardCode}/${categoryCode}`;
        } else {
            alert("처리 중 오류가 발생했습니다.");
        }
    })
        .catch(err => console.error("Error:", err));
}

/* ================================================
    [4] 목록 조회 & 페이징 기능
=================================================== */
// 페이지 이동
function changePage(pageNumber) {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.set('page', pageNumber);
    location.href = window.location.pathname + "?" + urlParams.toString();
}