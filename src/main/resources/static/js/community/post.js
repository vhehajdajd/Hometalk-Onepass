/*
    페이지 초기화 / 이벤트 연결 / submit 검증 / 카테고리 변경 / beforeunload
*/

window.quill = window.quill || null;
window.isSubmitting = window.isSubmitting || false;

document.addEventListener('DOMContentLoaded', () => {
    initEditor();
    bindPostFormSubmit();
    bindBeforeUnload();
    bindTitleCounter();
    bindCategoryChange();
    bindTempModal();
    bindButtons();
});

/* ======================
    초기화
====================== */

function initEditor() {
    const editorElement = document.getElementById('editor');

    if (editorElement && !window.quill && typeof initQuill === 'function') {
        initQuill();
    }
}

/* ======================
    게시글 등록 검증
====================== */

function bindPostFormSubmit() {
    const postForm = document.getElementById('postForm');
    if (!postForm) return;

    postForm.addEventListener('submit', (e) => {
        if (!validateTradeType()) {
            e.preventDefault();
            return;
        }

        if (!syncQuillContent()) {
            e.preventDefault();
            return;
        }

        window.isSubmitting = true;
    });
}

function validateTradeType() {
    const categorySelect = document.querySelector('.category-select');
    const tradeType = document.querySelector('select[name="tradeType"]');

    if (!categorySelect) return true;

    const selectedOption = categorySelect.options[categorySelect.selectedIndex];
    const categoryCode = selectedOption?.dataset.code;

    if (categoryCode === 'TRADE' && (!tradeType || !tradeType.value)) {
        alert('거래 유형을 선택해주세요.');
        tradeType?.focus();
        return false;
    }

    return true;
}

function syncQuillContent() {
    if (!window.quill) return true;

    const contentInput = document.getElementById('content');
    const htmlContent = window.quill.root.innerHTML;

    if (htmlContent === '<p><br></p>' || htmlContent.trim() === '') {
        alert('내용을 입력해주세요.');
        return false;
    }

    if (contentInput) {
        contentInput.value = htmlContent;
    }

    return true;
}

/* ======================
    페이지 이탈 방지
====================== */

function bindBeforeUnload() {
    window.addEventListener('beforeunload', (event) => {
        if (window.isSubmitting) return;

        const hasEditor = document.getElementById('editor');
        const hasContent = window.quill && window.quill.root.innerText.trim().length > 0;

        if (hasEditor && hasContent) {
            event.preventDefault();
            event.returnValue = '';
        }
    });
}

/* ======================
    제목 글자 수
====================== */

function bindTitleCounter() {
    const titleInput = document.getElementById('title');
    const charCount = document.getElementById('charCount');

    if (!titleInput || !charCount) return;

    charCount.innerText = titleInput.value.length;
    titleInput.addEventListener('input', () => {
        charCount.innerText = titleInput.value.length;
    });
}

/* ======================
    카테고리 변경
====================== */

function bindCategoryChange() {
    const categorySelect = document.querySelector('.category-select');
    if (!categorySelect) return;

    categorySelect.addEventListener('change', toggleTradeBox);
    toggleTradeBox();
}

function toggleTradeBox() {
    const categorySelect = document.querySelector('.category-select');
    const tradeBox = document.getElementById('tradeBox');
    const tradeType = document.querySelector('select[name="tradeType"]');

    if (!categorySelect) return;

    const selectedOption = categorySelect.options[categorySelect.selectedIndex];
    const categoryCode = selectedOption?.dataset.code?.trim().toLowerCase();
    const isTrade = categoryCode === 'trade';

    if (tradeBox) {
        tradeBox.style.display = isTrade ? 'block' : 'none';
    }

    if (tradeType) {
        tradeType.required = isTrade;
        tradeType.disabled = !isTrade;

        if (!isTrade) {
            tradeType.value = '';
            tradeType.setCustomValidity('');
        }
    }
}

/* ======================
    임시저장 모달
====================== */

function bindTempModal() {
    const btnLoadTemp = document.getElementById('btnLoadTemp');
    const closeBtn = document.querySelector('.close-modal');

    if (btnLoadTemp) {
        btnLoadTemp.addEventListener('click', () => {
            const boardCode = btnLoadTemp.dataset.boardCode || getBoardCode();
            loadTempList(boardCode);
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            const modal = document.getElementById('tempListModal');
            if (modal) modal.style.display = 'none';
        });
    }
}

/* ======================
    버튼 연결
====================== */

function bindButtons() {
    const btnSaveTemp = document.getElementById('btnSaveTemp');

    console.log('btnSaveTemp:', btnSaveTemp);
    console.log('saveTemp:', typeof saveTemp);

    if (btnSaveTemp) {
        const status = btnSaveTemp.dataset.status;

        if (status && status !== 'DRAFT') {
            btnSaveTemp.style.display = 'none';
        } else {
            btnSaveTemp.addEventListener('click', saveTemp);
        }
    }
}

/* ======================
    공통
====================== */

function getBoardCode() {
    return window.location.pathname.split('/')[3];
}