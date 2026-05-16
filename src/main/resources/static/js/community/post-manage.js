/*
    삭제 / 숨김 / 고정 / 상태 변경
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
async function toggleReaction(button, type) {
    const postId = button.dataset.postId;

    try {
        const response = await apiFetch(`/api/resident/${postId}/reaction?type=${type}`, {
            method: "POST"
        });
        const data = await response.json();

        // 버튼 상태
        button.classList.toggle("active", data[type.toLowerCase() + 'd']);

        // 숫자 업데이트
        const countElement = button.querySelector(`.${type.toLowerCase()}-count`);
        if (countElement && data[type.toLowerCase() + 'Count'] != null) {
            countElement.textContent = data[type.toLowerCase() + 'Count'];
        }

        // 반대 버튼 자동 해제
        const oppositeType = type === 'LIKE' ? 'DISLIKE' : 'LIKE';
        const oppositeBtn = document.querySelector(`.reaction-btn.${oppositeType.toLowerCase()}-btn[data-post-id="${postId}"]`);
        if (oppositeBtn) oppositeBtn.classList.remove("active");

    } catch (error) {
        console.error(error);
        showAlertModal("오류", "반응 처리 중 문제가 발생했습니다.");
    }
}