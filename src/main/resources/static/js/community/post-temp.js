/* ======================
    임시저장 (Save / Load / Delete / Count)
   ====================== */

// ======================
// 임시저장 저장
// ======================
async function saveTemp() {
    if (window.isSubmitting) return;

    const category = document.getElementById('categoryId')?.value || '';
    if (!category) {
        showAlertModal("카테고리를 선택해야 임시저장이 가능합니다.");
        document.getElementById('categoryId')?.focus();
        return;
    }

    showConfirmModal("임시저장하시겠습니까?", async () => {
        await saveTempProcess();
    });
}

async function saveTempProcess() {
    window.isSubmitting = true;

    const contentInput = document.getElementById('content');
    if (window.quill && contentInput) {
        contentInput.value = window.quill.root.innerHTML;
    }

    const form = document.getElementById('postForm');
    const formData = new FormData(form);

    if (!formData.get('id')) formData.delete('id');

    console.log('temp save id:', formData.get('id'));
    const boardCode = window.location.pathname.split('/')[3];

    try {
        const res = await formDataFetch(`/hometop/api/resident/${boardCode}/save-temp`, {
            method: 'POST',
            body: formData
        });

        if (!res.ok) {
            const errorText = await res.text();
            console.error("임시저장 실패 응답:", errorText);

            let message = "임시저장 실패";

            try {
                const errorData = JSON.parse(errorText);
                message = errorData.message || message;
            } catch (e) {
                message = errorText || message;
            }

            showAlertModal(message);
            return;
        }

        const data = await res.json();
        if (data.id) {
            showAlertModal(data.message);
            ensureIdInput(form, data.id);
            updateTempCount(boardCode);
        }
    } catch (err) {
        console.error("임시저장 에러:", err);
        showAlertModal("임시저장 중 오류가 발생했습니다.");
    } finally {
        window.isSubmitting = false;
    }
}

// ======================
// 임시저장 ID 보장
// ======================
function ensureIdInput(form, id) {
    let idInput = form.querySelector('input[name="id"]');
    if (!idInput) {
        idInput = document.createElement('input');
        idInput.type = 'hidden';
        idInput.name = 'id';
        form.appendChild(idInput);
    }
    idInput.value = id;
}

// ======================
// 임시저장 카운트 업데이트
// ======================
async function updateTempCount(boardCode) {
    try {
        const res = await apiFetch(`/hometop/api/resident/${boardCode}/temp-count`);
        const count = await res.text();
        const tempCountDisplay = document.getElementById('temp-count-display');
        if (tempCountDisplay) {
            tempCountDisplay.innerText = count;
        }
    } catch (err) {
        console.error("카운트 업데이트 실패:", err);
    }
}

// ======================
// 날짜 포맷
// ======================
function formatDate(dateString) {
    if (!dateString) return '';
    const d = new Date(dateString);
    const pad = n => n.toString().padStart(2, '0');
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// ======================
// 임시저장 목록 조회
// ======================
function escapeHtml(str) {
    return str?.replace(/[&<>"']/g, m => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[m])) || '';
}

async function loadTempList(boardCode) {
    try {
        const res = await apiFetch(`/hometop/api/resident/${boardCode}/temp-list`);
        if (!res.ok) throw new Error('목록을 불러오는데 실패했습니다.');
        const data = await res.json();

        const listArea = document.getElementById('tempListArea');
        const countBadge = document.getElementById('tempCountBadge');
        listArea.innerHTML = '';
        countBadge && (countBadge.innerText = data.length);

        if (!data.length) {
            listArea.innerHTML = '<li class="no-data">임시저장된 글이 없습니다.</li>';
        } else {
            listArea.innerHTML = data.map(post => `
                <li onclick="window.isSubmitting=true; location.href='/hometop/community/${boardCode}/edit/${post.id}'" style="cursor:pointer;">
                    <div>
                        <span class="temp-category">[${post.categoryName || '미지정'}]</span>
                        <span class="temp-title">${escapeHtml(post.title) || '제목 없음'}</span>
                        <span class="temp-date">${formatDate(post.createdAt)}</span>
                    </div>
                    <button type="button" class="btn-delete-temp" onclick="deleteTempPost(event, ${post.id}, '${boardCode}')">
                        &#128465;
                    </button>
                </li>
            `).join('');
        }

        document.getElementById('tempListModal').style.display = 'flex';
    } catch (err) {
        console.error(err);
        showAlertModal('임시저장 목록을 가져오는 중 오류가 발생했습니다.');
    }
}

// ======================
// 임시저장 삭제
// ======================
async function deleteTempPost(event, id, boardCode) {
    event.stopPropagation();
    showConfirmModal("삭제하시겠습니까?", async () => {
        try {
            const res = await apiFetch(`/hometop/api/resident/${boardCode}/delete-temp/${id}`, {
                method: 'POST',
                headers: getCsrfOnlyHeaders()
            });

            if (res.ok) {
                showAlertModal("삭제되었습니다.");
                loadTempList(boardCode);
            } else if (res.status === 403) {
                showAlertModal("삭제 권한이 없거나 세션이 만료되었습니다. (403)");
            } else {
                showAlertModal("삭제 실패");
            }
        } catch (err) {
            console.error(err);
            showAlertModal("삭제 중 오류가 발생했습니다.");
        }
    });
}