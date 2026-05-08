console.log("댓글 JS 로드 완료");

function toggleEditForm(commentId) {
    console.log("댓글 수정 모드 전환 (ID: ", commentId);

    const editForm = document.getElementById('edit-form-' + commentId);   // 수정 폼 영역
    const reviewBody = document.getElementById('body-' + commentId);     // 본문 내용 영역
    const metaArea = document.getElementById('meta-' + commentId);       // 닉네임/버튼 영역

    if (editForm.style.display === 'none') {
        // 수정 모드 켜기
        editForm.style.display = 'block';
        reviewBody.style.display = 'none';
        metaArea.style.display = 'none';

        // 커서 자동 포커스 (선택 사항)
        editForm.querySelector('textarea').focus();
    } else {
        // 수정 모드 끄기 (취소)
        editForm.style.display = 'none';
        reviewBody.style.display = '';
        metaArea.style.display = '';
    }
}

/* 더보기 버튼 - 댓글 표시 */
function showAllComments() {
    // 1. 숨겨진 댓글 찾기
    const hiddenComments = document.querySelectorAll('.review-hidden');

    // 2. 앞에서부터 10개만 표시
    const limit = 10;
    for (let i = 0; i < Math.min(hiddenComments.length, limit); i++) {
        hiddenComments[i].classList.remove('review-hidden');
    }

    // 3. 숨겨진 댓글 더 없으면 버튼 숨기기
    const remainingCount = document.querySelectorAll('.review-hidden').length;
    if (remainingCount === 0) {
        const moreBtn = document.getElementById('btnMoreComments');
        if (moreBtn) { moreBtn.style.display = 'none'; }
    }
}

// 댓글 드롭다운
function toggleCommentMenu(btn) {
    // 모든 열려있는 댓글 메뉴 닫기 (하나만 열리게)
    document.querySelectorAll('.drop-content').forEach(el => {
        if (el !== btn.nextElementSibling) el.classList.remove('show-menu');
    });
    // 클릭한 메뉴 토글
    btn.nextElementSibling.classList.toggle('show-menu');
}

// 바깥 영역 클릭 시 메뉴 닫기
window.addEventListener('click', function(e) {
    if (!e.target.closest('.custom-dropdown')) {
        document.querySelectorAll('.drop-content').forEach(el => {
            el.classList.remove('show-menu');
        });
    }
});