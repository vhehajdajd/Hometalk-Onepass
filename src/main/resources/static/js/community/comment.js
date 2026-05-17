function toggleEditForm(commentId) {
    const allEditForms = document.querySelectorAll('[id^="edit-form-"]');
    const allBodies = document.querySelectorAll('[id^="body-"]');
    const allMetas = document.querySelectorAll('[id^="meta-"]');

    const targetForm = document.getElementById('edit-form-' + commentId);
    const targetBody = document.getElementById('body-' + commentId);
    const targetMeta = document.getElementById('meta-' + commentId);

    if (!targetForm || !targetBody || !targetMeta) return;

    const isAlreadyOpen = (targetForm.style.display === 'block');

    allEditForms.forEach(f => f.style.display = 'none');
    allBodies.forEach(b => b.style.display = '');
    allMetas.forEach(m => m.style.display = '');

    if (!isAlreadyOpen) {
        targetForm.style.display = 'block';
        targetBody.style.display = 'none';
        targetMeta.style.display = 'none';

        const textarea = targetForm.querySelector('textarea');
        if (textarea) {
            textarea.focus();
            textarea.selectionStart = textarea.selectionEnd = textarea.value.length;
        }
    }
}

/* 더보기 버튼 - 댓글 표시 */
function showAllComments() {
    const hiddenComments = document.querySelectorAll('.review-hidden');
    const limit = 10;

    for (let i = 0; i < Math.min(hiddenComments.length, limit); i++) {
        hiddenComments[i].classList.remove('review-hidden');
    }

    const remainingCount = document.querySelectorAll('.review-hidden').length;
    if (remainingCount === 0) {
        const moreBtn = document.getElementById('btnMoreComments');
        if (moreBtn) { moreBtn.style.display = 'none'; }
    }
}

// 댓글 드롭다운
function toggleCommentMenu(btn) {
    document.querySelectorAll('.drop-content').forEach(el => {
        if (el !== btn.nextElementSibling) el.classList.remove('show-menu');
    });
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