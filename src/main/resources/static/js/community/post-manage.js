/*
    삭제 / 숨김 / 고정 / 상태 변경 / 신고
 */

// 게시글 삭제 (Soft Delete)
function deletePost() {
    const postId = this.dataset.id;
    showConfirmModal(
        "정말 삭제하시겠습니까?\n삭제된 글은 목록에서 사라집니다.",
        () => {
            const deleteForm = document.getElementById('deleteForm');
            if (deleteForm) {
                console.log(postId + "번 게시글 삭제 요청");
                deleteForm.submit();
            } else {
                console.error("삭제 폼(deleteForm)을 찾을 수 없습니다.");
            }
        }
    );
}

// 상단 고정
async function togglePin(postId) {
    const pinBtn = document.getElementById('pinBtn');
    const isCurrentlyPinned = pinBtn.classList.contains('active');
    const actionText = isCurrentlyPinned
        ? "고정 해제"
        : "상단 고정";
    showConfirmModal(
        `이 게시글을 ${actionText}하시겠습니까?`,
        async () => {
            try {
                const response = await apiFetch(
                    `/hometop/api/posts/${postId}/pin`,
                    {
                        method: 'POST'
                    }
                );
                if (response.ok) {
                    showAlertModal(`${actionText} 되었습니다.`);
                    location.reload();
                } else {
                    showAlertModal("처리 중 오류가 발생했습니다.");
                }
            } catch (err) {
                console.error(err)
                showAlertModal("서버 오류가 발생했습니다.");
            }
        }
    );
}

// 숨김 처리
function hidePost(postId) {
    showConfirmModal(
        "정말 숨기시겠습니까?",
        async () => {
            try {
                const response = await apiFetch(
                    `/hometop/api/posts/${postId}/hide`,
                    {
                        method: 'POST'
                    }
                );

                if (response.ok) {
                    showAlertModal("숨김 처리가 완료되었습니다.");
                    location.href = `/hometop/community/square/all`;
                } else {
                    showAlertModal("처리 중 오류가 발생했습니다.");
                }
            } catch (err) {
                console.error(err);
                showAlertModal("서버 오류가 발생했습니다.");
            }
        }
    );
}

// 숨김 해제
function unhidePost(postId) {
    showConfirmModal(
        "이 게시글을 다시 노출하시겠습니까?",
        async () => {
            try {
                const response = await apiFetch(
                    `/hometop/api/posts/${postId}/unhide`,
                    {
                        method: 'POST'
                    }
                );

                if (response.ok) {
                    showAlertModal("숨김이 해제되었습니다.");
                    location.reload();
                } else {
                    showAlertModal("처리 중 오류가 발생했습니다.");
                }
            } catch (err) {
                console.error(err);
                showAlertModal("서버 오류가 발생했습니다.");
            }
        }
    );
}

// 상태 변경
async function updateStatus(postId, status) {
    try {
        const response = await apiFetch(
            `/hometop/api/resident/${postId}/status`,
            {
                method: 'POST',
                body: JSON.stringify({
                    marketStatus: status
                })
            }
        );
        if (response.ok) {
            showAlertModal("상태가 변경되었습니다.");
            location.reload();
        } else {
            console.error("Error Status:", response.status);
            showAlertModal("변경 실패");
        }
    } catch (err) {
        console.error(err);
        showAlertModal("서버 오류가 발생했습니다.");
    }
}

async function updateTradeStatus(postId, status) {
    try {
        const response = await apiFetch(
            `/hometop/api/resident/${postId}/trade/status`,
            {
                method: 'POST',
                body: JSON.stringify({
                    tradeStatus: status
                })
            }
        );
        if (response.ok) {
            showAlertModal("거래 상태가 변경되었습니다.");
            location.reload();
        } else {
            showAlertModal("변경 실패");
        }
    } catch (err) {
        console.error(err);
        showAlertModal("서버 오류가 발생했습니다.");
    }
}


// 1. 신고 모달 열기
function openReportModal(postId) {
    const modal = document.getElementById('reportModal');
    if (!modal) return;

    // 모달 내부 데이터 초기화
    document.getElementById('reportPostId').value = postId;
    document.getElementById('reportReason').value = 'SPAM'; // 기본값 설정
    document.getElementById('reportDetail').value = '';

    // 모달 표시
    modal.classList.add('show');
    document.addEventListener('keydown', handleReportModalEsc);
}


// 2. 신고 모달 닫기
function closeReportModal() {
    const modal = document.getElementById('reportModal');
    if (!modal) return;

    modal.classList.remove('show');
    document.removeEventListener('keydown', handleReportModalEsc);
}


// ESC 키 누를 때 모달 닫히는 핸들러
function handleReportModalEsc(e) {
    if (e.key === 'Escape') {
        closeReportModal();
    }
}


// 3. 신고 데이터 전송 (접수)
async function submitReport() {
    const postId = document.getElementById('reportPostId').value;
    const reason = document.getElementById('reportReason').value;
    const detail = document.getElementById('reportDetail').value.trim();

    // 유효성 검사
    if (!detail) {
        showAlertModal('상세 신고 사유를 입력해주세요.');
        return;
    }

    const message = '이 게시글을 신고하시겠습니까?\n허위 신고일 경우 서비스 이용이 제한될 수 있습니다.';

    showConfirmModal(message, async () => {
        try {
            const requestData = {
                postId: parseInt(postId),
                reason: reason,
                detail: detail
            };

            const response = await apiFetch('/hometop/api/posts/reports', {
                method: 'POST',
                body: JSON.stringify(requestData)
            });

            if (response.ok) {
                showAlertModal('신고가 정상적으로 접수되었습니다. 운영자 검토까지는 시간이 소요될 수 있습니다.');
                closeReportModal();
            } else {
                showAlertModal('신고 접수 중 오류가 발생했습니다. 다시 시도해 주세요.');
            }
        } catch (error) {
            console.error('신고 접수 실패:', error);
            showAlertModal('서버와 통신 중 에러가 발생했습니다.');
        }
    });
}

// 취소 버튼 컨펌
function confirmCancel() {
    if (isSubmitting) {
        return true;
    }
    showConfirmModal(
        "작성 중인 내용을 중단하고 목록으로 돌아가시겠습니까?\n(임시저장된 내용은 보존됩니다.)",
        () => {
            isSubmitting = true;
            const cancelLink = document.querySelector('.btn-outline');
            if (cancelLink) {
                location.href = cancelLink.href;
            }
        }
    );
    return false;
}

// 페이지 이동
function changePage(pageNumber) {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.set('page', pageNumber);
    location.href = window.location.pathname + "?" + urlParams.toString();
}

// 좋아요
function toggleReaction(buttonElement, type) {
    const postId = buttonElement.dataset.postId;
    const url = type === 'LIKE'
        ? `/hometop/api/resident/${postId}/like`
        : `/hometop/api/resident/${postId}/dislike`;

    fetch(url, { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            if(data.code && data.code !== "C999") {
                alert(data.message);
                return;
            }

            const likeBtn = document.querySelector(`.like-btn[data-post-id="${postId}"]`);
            const dislikeBtn = document.querySelector(`.dislike-btn[data-post-id="${postId}"]`);

            if (likeBtn) {
                likeBtn.querySelector('.like-count').innerText = data.likeCount;
                likeBtn.classList.toggle('active', data.liked);
                const icon = likeBtn.querySelector('i');
                icon.classList.remove('fa-regular', 'fa-solid');
                icon.classList.add(data.liked ? 'fa-solid' : 'fa-regular', 'fa-heart');
            }

            if (dislikeBtn) {
                dislikeBtn.querySelector('.dislike-count').innerText = data.dislikeCount;
                dislikeBtn.classList.toggle('active', data.disliked);
                const icon = dislikeBtn.querySelector('i');
                icon.classList.remove('fa-regular', 'fa-solid');
                icon.classList.add(data.disliked ? 'fa-solid' : 'fa-regular', 'fa-thumbs-down');
            }
        })
        .catch(err => console.error('추천 처리 중 오류 발생:', err));
}

// HTML의 th:onclick이나 외부 모듈에서 호출 가능하도록 전체 윈도우 객체 바인딩 세트 배치
window.deletePost = deletePost;
window.togglePin = togglePin;
window.hidePost = hidePost;
window.unhidePost = unhidePost;
window.updateStatus = updateStatus;
window.updateTradeStatus = updateTradeStatus;
window.openReportModal = openReportModal;
window.closeReportModal = closeReportModal;
window.submitReport = submitReport;
window.confirmCancel = confirmCancel;
window.changePage = changePage;
window.toggleReaction = toggleReaction;