/* ================================================
    [1] 페이지 초기화 & 이벤트 바인딩
=================================================== */
let quill;
let isSubmitting = false;

/* 안전장치로 DOMContentLoaded로 묶음 */
document.addEventListener('DOMContentLoaded', () => {

    const editorElement = document.getElementById('editor');
    if (editorElement && !quill) { // quill이 아직 없을 때만 초기화
        initQuill();
    }

    const postForm = document.getElementById('postForm');
    const contentInput = document.getElementById('content');

    if (postForm) {
        postForm.addEventListener('submit', (e) => {
            if (quill) {
                const htmlContent = quill.root.innerHTML;
                if (htmlContent === '<p><br></p>' || htmlContent.trim() === '') {
                    alert("내용을 입력해주세요.");
                    e.preventDefault();
                    return false;
                }
                document.getElementById('content').value = htmlContent;
            }
            isSubmitting = true;
        });
    }

    window.addEventListener('beforeunload', (event) => {
        const editorElement = document.getElementById('editor');
        if (editorElement && !isSubmitting) {
            event.preventDefault();
            event.returnValue = '';
        }
    });

    // 제목 글자 수 카운트 (기존 로직)
    const titleInput = document.getElementById('title');
    if (titleInput) {
        titleInput.addEventListener('input', updateCharCount);
        document.getElementById('charCount').innerText = titleInput.value.length;
    }

    // 3. 임시저장 목록 모달 열기
    const btnLoadTemp = document.getElementById('btnLoadTemp');
    if (btnLoadTemp) {
        btnLoadTemp.addEventListener('click', function() {
            const boardCode = this.dataset.boardCode || 'default';
            loadTempList(boardCode);
        });
    }

    // 4. 모달 닫기
    const closeBtn = document.querySelector('.close-modal');
    if (closeBtn) {
        closeBtn.onclick = () => {
            document.getElementById('tempListModal').style.display = 'none';
        };
    }

    // 5. 삭제 버튼 (상세 페이지)
    const btnDelete = document.getElementById('btn-delete');
    if (btnDelete) {
        btnDelete.addEventListener('click', deletePost);
    }

    // 6. 임시저장 실행 버튼 (작성 페이지)
    const btnSaveTemp = document.getElementById('btnSaveTemp');
    if (btnSaveTemp) {
        btnSaveTemp.addEventListener('click', saveTemp);
    }
});

/* ================================================
    [2] Quill 에디터 설정 & 이미지 업로드
=================================================== */

function initQuill() {
    const editor = document.getElementById('editor');
    if (!editor) return;

    quill = new Quill('#editor', {
        theme: 'snow',
        modules: {
            toolbar: {
                container: [
                    ['bold', 'italic', 'underline'],
                    [{ 'align': [] }],
                    [{ 'size': ['small', false, 'large', 'huge'] }],
                    [{ 'color': [] }],
                    ['link', 'image']
                ],
                handlers: { image: imageHandler }
            },
            imageResize: { displaySize: true }
        }
    });
    const content = document.getElementById('content')?.value;
    if (content && content.trim() !== '') {
        quill.root.innerHTML = content;
    }

    quill.root.addEventListener('paste', handlePaste, false);
    quill.root.addEventListener('drop', handleDrop, false);
}

function imageHandler() {
    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.setAttribute('accept', 'image/*');
    input.click();

    input.onchange = async () => {
        const file = input.files[0];
        const formData = new FormData();
        formData.append('file', file);

        // 기존 코드에 있는 CSRF 토큰 활용
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;

        try {
            const res = await fetch('/hometop/community/image-upload', {
                method: 'POST',
                headers: { [header]: token },
                body: formData
            });
            const data = await res.json();

            const range = quill.getSelection();
            quill.insertEmbed(range.index, 'image', data.url);
        } catch (err) {
            console.error("이미지 업로드 실패:", err);
        }
    };
}

// 붙여넣기
function handlePaste(e) {
    const clipboard = e.clipboardData;
    if (clipboard && clipboard.files && clipboard.files.length) {
        e.preventDefault();
        [...clipboard.files].forEach(file => {
            if (file.type.match(/^image\//)) {
                uploadImageFile(file);
            }
        });
    }
}

// 드래그 드롭
function handleDrop(e) {
    e.preventDefault();
    if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length) {
        [...e.dataTransfer.files].forEach(file => {
            if (file.type.match(/^image\//)) {
                uploadImageFile(file);
            }
        });
    }
}

// 공통 업로드 함수
async function uploadImageFile(file) {
    const formData = new FormData();
    formData.append('file', file);

    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;

    try {
        const res = await fetch('/hometop/community/image-upload', {
            method: 'POST',

            headers: { [header]: token },
            body: formData
        });
        const data = await res.json();
        const range = quill.getSelection() || { index: quill.getLength() };
        quill.insertEmbed(range.index, 'image', data.url);
    } catch (err) {
        console.error("이미지 업로드 실패:", err);
    }
}


/* ================================================
    [3] 게시글 작성 & 임시저장 기능
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

    if (confirm("임시저장하시겠습니까?")) {
        const contentInput = document.getElementById('content');
        if (quill && contentInput) {
            contentInput.value = quill.root.innerHTML;
        }

        const form = document.getElementById('postForm');
        const formData = new FormData(form);

        const pathSegments = window.location.pathname.split('/');
        const boardCode = pathSegments[3];

        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;

        fetch(`/hometop/community/${boardCode}/save-temp`, {
            method: 'POST',
            headers: { [header]: token },
            body: formData
        })
            .then(res => {
                if (!res.ok) throw new Error("임시저장 실패");
                return res.json();
            })
            .then(data => {
                if (data.id) {
                    alert(data.message);
                    let idInput = document.querySelector('input[name="id"]');
                    if (!idInput) {
                        idInput = document.createElement('input');
                        idInput.type = 'hidden';
                        idInput.name = 'id';
                        form.appendChild(idInput);
                    }
                    idInput.value = data.id;
                }
            })
            .catch(err => {
                console.error("임시저장 에러:", err);
                alert("임시저장 중 오류가 발생했습니다.");
            });
    }
}

// 날짜 포맷팅
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');

    // 포맷: 04-21 16:44 (연도까지 필요하면 앞에 date.getFullYear() 추가)
    return `${month}-${day} ${hours}:${minutes}`;
}

// 임시저장 목록 호출
function loadTempList(boardCode) {
    fetch(`/hometop/community/${boardCode}/temp-list`)
        .then(response => {
            if (!response.ok) throw new Error('목록을 불러오는데 실패했습니다.');
            return response.json();
        })
        .then(data => {
            const listArea = document.getElementById('tempListArea');
            listArea.innerHTML = '';

            const countBadge = document.getElementById('tempCountBadge');
            if (countBadge) {
                countBadge.innerText = data.length;
            }

            if (!data || data.length === 0) {
                listArea.innerHTML = '<li class="no-data">임시저장된 글이 없습니다.</li>';
            } else {
                const html = data.map(post => `
                        <li onclick="location.href='/hometop/community/${boardCode}/edit/${post.id}'" style="cursor:pointer;">
                            <div>
                                <span class="temp-category">[${post.categoryName || '미지정'}]</span>
                                <span class="temp-title">${post.title || '제목 없음'}</span>
                                <span class="temp-date">${formatDate(post.createdAt)}</span>
                            </div>
                            <button type="button" class="btn-delete-temp" onclick="deleteTempPost(event, ${post.id}, '${boardCode}')">
                                &#128465;
                            </button>
                        </li>
                    `).join('');
                listArea.innerHTML = html;
            }
            const modal = document.getElementById('tempListModal');
            modal.style.display = 'flex';
        })
        .catch(err => {
            console.error(err);
            alert('임시저장 목록을 가져오는 중 오류가 발생했습니다.');
        });
}

// 임시저장 삭제용 함수
function deleteTempPost(event, id, boardCode) {
    event.stopPropagation();

    if (!confirm("삭제하시겠습니까?")) return;

    // 1. 메타 태그에서 CSRF 토큰과 헤더 이름 가져오기
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;

    fetch(`/hometop/community/${boardCode}/delete-temp/${id}`, {
        method: 'POST',
        headers: {
            // 2. 헤더에 토큰 실어보내기
            [header]: token
        }
    })
        .then(res => {
        if (res.ok) {
            alert("삭제되었습니다.");
            loadTempList(boardCode);
        } else if (res.status === 403) {
            alert("삭제 권한이 없거나 세션이 만료되었습니다. (403)");
        } else {
            alert("삭제 실패");
        }
    })
        .catch(err => console.error("Error:", err));
}

// 임시저장한 글을 등록 후 목록에서 지우기
// 게시글 등록 버튼 클릭 시
function submitPost() {
    const formData = new FormData(document.getElementById('postForm'));

    fetch(`/hometop/community/${boardCode}/save`, {
        method: 'POST',
        body: formData,
        headers: { [header]: token }
    })
        .then(res => {
        if (res.ok) {
            // 등록 성공 시 임시저장 카운트 초기화
            const countBadge = document.getElementById('tempCountBadge');
            if (countBadge) countBadge.innerText = '0';

            alert("등록되었습니다.");
            location.href = `/community/${boardCode}`; // 목록으로 이동
        }
    });
}

/* ================================================
    [4] 게시글 상세 & 삭제 기능
=================================================== */
// 취소 버튼 컨펌
function confirmCancel() {
    const result = confirm("작성 중인 내용을 중단하고 목록으로 돌아가시겠습니까?\n(임시저장된 내용은 보존됩니다.)");
    if(result) {
        isSubmitting = true;
    }
    return result;
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
    const pinBtn = document.getElementById('pinBtn');
    const isCurrentlyPinned = pinBtn.classList.contains('active');
    const actionText = isCurrentlyPinned ? "고정 해제" : "상단 고정";

    if (!confirm(`이 게시글을 ${actionText}하시겠습니까?`)) return;

    const tokenTag = document.querySelector('meta[name="_csrf"]');
    const headerTag = document.querySelector('meta[name="_csrf_header"]');

    if (!tokenTag || !headerTag) {
        console.error("CSRF 태그를 찾을 수 없습니다. 로그인이 되어 있나요?");
        return;
    }

    const token = tokenTag.content;
    const header = headerTag.content;

    fetch(`/hometop/api/posts/${postId}/pin`, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json",
            [header]: token
        }
    })
        .then(response => {
        if (response.ok) {
            alert(`${actionText} 되었습니다.`);
            location.reload(); // UI 업데이트를 위해 새로고침
        } else {
            alert("처리 중 오류가 발생했습니다.");
        }
    })
        .catch(err => console.error("Error:", err));
}

// 숨김 처리
function hidePost(postId) {
    if(!confirm("정말 숨기시겠습니까?")) return;

    // 1. 메타 태그에서 CSRF 토큰과 헤더 이름 가져오기
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    const adminArea = document.querySelector('.admin-tools');
    const boardCode = adminArea.dataset.boardCode;
    const categoryCode = adminArea.dataset.categoryCode || 'all';

    fetch(`/hometop/api/posts/${postId}/hide`, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json",
            [header]: token
        }
    })
        .then(response => {
        if (response.ok) {
            alert("숨김 처리가 완료되었습니다.");
            // 원래 있던 게시판 목록으로 이동
            location.href = `/hometop/community/square/all`;
        } else {
            alert("처리 중 오류가 발생했습니다.");
        }
    })
        .catch(err => console.error("Error:", err));
}

// 숨김 해제
function unhidePost(postId) {
    if (!confirm("이 게시글을 다시 노출하시겠습니까?")) return;

    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    fetch(`/hometop/api/posts/${postId}/unhide`, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json",
            [header]: token
        }
    })
        .then(response => {
        if (response.ok) {
            alert("숨김이 해제되었습니다.");
            location.reload();
        } else {
            alert("처리 중 오류가 발생했습니다.");
        }
    })
        .catch(err => console.error("Error:", err));
}

/* ================================================
    [5] 목록 조회 & 페이징 기능
=================================================== */
// 페이지 이동
function changePage(pageNumber) {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.set('page', pageNumber);
    location.href = window.location.pathname + "?" + urlParams.toString();
}

// 상태 변경 함수
function updateStatus(postId, status) {
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;

    fetch(`/hometop/api/posts/${postId}/status`, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json",
            [header]: token
        },
        body: JSON.stringify({ marketStatus: status })
    }).then(res => {
        if (res.ok) {
            alert("상태가 변경되었습니다.");
            location.reload();
        } else {
            console.error("Error Status:", res.status);
            alert("변경 실패. 컨트롤러 주소를 확인하세요.");
        }
    });
}

/* ================================================
    [6] 태그
=================================================== */
const tagInput = document.querySelector('#tagInput');
const tagList = document.querySelector('#tag-list');
const hiddenTags = document.querySelector('#hidden-tags');
let tags = window.initialTags || [];

document.addEventListener('DOMContentLoaded', () => {
    // 수정 모드 - 기존 태그 보여줌
    if (tags.length > 0) {
        renderTags();
    }
});

if (tagInput) {
    tagInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            const tagName = tagInput.value.trim();

            if (tagName.length > 5) {
                alert("태그는 최대 5자까지만 입력 가능합니다.");
                return;
            }

            if (tagName && !tags.includes(tagName)) {
                tags.push(tagName);
                renderTags();
            }
            tagInput.value = '';
            return false;
        }
    });
}

function renderTags() {
    if (!tagList || !hiddenTags) return;

    tagList.innerHTML = '';
    hiddenTags.innerHTML = '';

    tags.forEach((tag, index) => {
        const span = document.createElement('span');
        span.className = 'tag-badge';
        span.innerHTML = `${tag} <i class="remove-tag" onclick="removeTag(${index})">&times;</i>`;
        tagList.appendChild(span);

        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'tags';
        input.value = tag;
        hiddenTags.appendChild(input);
    });
}

function removeTag(index) {
    tags.splice(index, 1);
    renderTags();
}

/* ================================================
    [7] 태그 자동완성 (Auto-complete)
=================================================== */
const suggestions = document.getElementById('tagSuggestions');

if (tagInput) {
    // 1. 입력 시 자동완성 목록 조회
    tagInput.addEventListener('keyup', (e) => {
        // 엔터키일 때는 자동완성 로직 건너뛰기 (기존 keydown 로직이 처리)
        if (e.key === 'Enter') return;

        const keyword = tagInput.value.trim();

        if (keyword.length < 1) {
            if (suggestions) suggestions.style.display = 'none';
            return;
        }

        fetch(`/hometop/api/posts/tags/search?keyword=${encodeURIComponent(keyword)}`)
            .then(res => res.json())
            .then(data => {
            if (data && data.length > 0) {
                // 데이터가 있을 경우 목록 생성
                suggestions.innerHTML = data.map(name =>
                `<li style="cursor:pointer; padding:8px 12px; border-bottom:1px solid #eee;"
                     onclick="selectTag('${name}')">${name}</li>`
                ).join('');
                suggestions.style.display = 'block';
            } else {
                suggestions.style.display = 'none';
            }
        })
            .catch(err => console.error("태그 검색 에러:", err));
    });

    // 2. 입력창 외부 클릭 시 목록 닫기
    document.addEventListener('click', (e) => {
        if (e.target !== tagInput && e.target !== suggestions) {
            if (suggestions) suggestions.style.display = 'none';
        }
    });
}

// 3. 자동완성 목록에서 태그 선택 시
function selectTag(name) {
    if (tagInput && suggestions) {
        // 이미 추가된 태그인지 확인 (기존 tags 배열 활용)
        if (!tags.includes(name)) {
            tags.push(name);
            renderTags();
        }
        tagInput.value = '';
        suggestions.style.display = 'none';
        tagInput.focus();
    }
}