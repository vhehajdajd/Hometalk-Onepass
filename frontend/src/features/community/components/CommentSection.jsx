import { useEffect, useState } from "react";

const apiFetch = window.apiFetch;
const showAlertModal = window.showAlertModal;
const showConfirmModal = window.showConfirmModal;

export default function CommentSection({ postId, authenticated }) {

    // state 변수 선언 : useState() 함수 사용 → 모두 초기값 할당
    const [comments, setComments] = useState([]);
    const [content, setContent] = useState("");
    const [editingId, setEditingId] = useState(null);
    const [editContent, setEditContent] = useState("");
    const [visibleCount, setVisibleCount] = useState(10);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);

    // 댓글 최초 조회용
    useEffect(() => {
        fetchComments();
    }, []);

    // 컴포넌트 처음 렌더링 시 1번 실행
    useEffect(() => {
        // 바깥 영역 클릭 시 실행될 함수
        const handleOutsideClick = (e) => {
            // 클릭한 요소가 .custom-dropdown 내부가 아니면
            if (!e.target.closest(".custom-dropdown")) {
                // 열려 있는 모든 드롭다운 메뉴 찾음
                document.querySelectorAll(".drop-content")
                    // 순회 후 닫기
                    .forEach(el => {
                        el.classList.remove("show-menu");
                    });
            }
        };
        // 브라우저 window에 click 이벤트 등록 → 화면 어디든 클릭하면 handleOutsideClick 실행
        window.addEventListener("click", handleOutsideClick);

        return () => {
            window.removeEventListener("click", handleOutsideClick);
        };
    }, []); // 첫 마운트 시 한 번만 실행

    const showMessage = (message) => {
        if (showAlertModal) showAlertModal(message);
        else alert(message);
    };

    const fetchComments = async () => {
        try {
            const response = await apiFetch(`/hometop/api/community/posts/${postId}/comments`);

            if (!response.ok) {
                throw new Error("댓글 조회 실패");
            }

            const data = await response.json();
            setComments(data);
        } catch (error) {
            console.error(error);
            showMessage("댓글을 불러오지 못했습니다.");
        } finally {
            setLoading(false);
        }
    };

    // 댓글 등록 함수 : form submit 시 실행
    const submitComment = async (e) => {
        // form 기본 동작(새로고침) 막기
        e.preventDefault();

        const trimmed = content.trim(); // 공백 제거
        if (!trimmed) {                        // 빈 댓글 방지
            showMessage("댓글 내용을 입력해주세요.");
            return;
        }

        // 중복 클릭 방지 - 등록 버튼 disabled 처리
        setSubmitting(true);

        try {
            // 댓글 등록 API 호출
            const response = await apiFetch(`/hometop/api/community/posts/${postId}/comments`, {
                method: "POST",
                body: JSON.stringify({ content: trimmed }),
            });

            // 실패 응답 처리
            if (!response.ok) {
                throw new Error("댓글 등록 실패");
            }
            // 최신 댓글 목록 받음
            const data = await response.json();
            setComments(data);          // 댓글 state 갱신 - rerender
            setContent("");      // 입력 초기화
            setVisibleCount(Math.max(10, data.length));     // 댓글 추가 시 더보기 개수 보정
            showMessage("댓글이 등록되었습니다.");
        } catch (error) {
            console.error(error);       // 콘솔 에러 확인용
            showMessage("댓글 등록 중 오류가 발생했습니다.");
        } finally {
            setSubmitting(false); // 성공/실패 상관없이 다시 버튼 활성화
        }
    };

    // 수정 버튼 클릭 시 실행
    const startEdit = (comment) => { // editingId === comment.id일 때만 내용 불러오기
        setEditingId(comment.id);           // 현재 수정 중인 댓글 id 저장
        setEditContent(comment.content);    // 기존 댓글 내용 넣기
    };

    // 수정 취소 버튼
    const cancelEdit = () => {
        setEditingId(null);     // 수정 상태 초기화
        setEditContent("");     // 내용 비우기
    };

    // 댓글 수정 저장
    const saveEdit = async (commentId) => {
        const trimmed = editContent.trim();     // 공백 제거

        if (!trimmed) {     // 빈 값 방지
            showMessage("댓글 내용을 입력해주세요.");
            return;
        }

        setSubmitting(true);        // 중복 요청 방지

        try {
            // 수정 API 호출
            const response = await apiFetch(
                `/hometop/api/community/posts/${postId}/comments/${commentId}/edit`,
                {
                    method: "POST",
                    body: JSON.stringify({ content: trimmed }),
                }
            );

            if (!response.ok) {
                throw new Error("댓글 수정 실패");
            }

            const data = await response.json();     // 최신 댓글 목록 다시 받기
            setComments(data);                      // 화면 갱신
            cancelEdit();                           // 수정 모드 종료
            showMessage("댓글이 수정되었습니다.");
        } catch (error) {
            console.error(error);
            showMessage("댓글 수정 중 오류가 발생했습니다.");
        } finally {
            setSubmitting(false);            // 버튼 활성화
        }
    };

    // 댓글 삭제 함수
    const deleteComment = (commentId) => {
        // confirm 눌렀을 때 실행
        const runDelete = async () => {
            setSubmitting(true);    // 삭제 중 버튼 비활성화

            try {
                // 삭제 API 요청
                const response = await apiFetch(
                    `/hometop/api/community/posts/${postId}/comments/${commentId}/delete`,
                    {
                        method: "POST",
                    }
                );

                // 실패 시 에러
                if (!response.ok) {
                    throw new Error("댓글 삭제 실패");
                }

                const data = await response.json();     // 최신 댓글 목록 다시 받기
                setComments(data);                      // 댓글 state 갱신 - rerender
                setVisibleCount(10);             // 더보기 버튼 개수 초기화
                showMessage("댓글이 삭제되었습니다.");     // 성공 모달
            } catch (error) {
                console.error(error);
                showMessage("댓글 삭제 중 오류가 발생했습니다.");
            } finally {
                setSubmitting(false);
            }
        };

        // custom confirm modal
        if (showConfirmModal) {
            // 확인 누를 시 runDelete 실행
            showConfirmModal("댓글을 삭제하시겠습니까?", runDelete);
        } else if (confirm("댓글을 삭제하시겠습니까?")) {      // confirm modal 실행 안 될 때 fallback
            runDelete();    // 브라우저 기본 confirm에서 확인 누를 시 실행
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return "";

        const date = new Date(dateString);
        const pad = (n) => String(n).padStart(2, "0");

        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
    };

    const visibleComments = comments.slice(0, visibleCount);
    const hasMore = comments.length > visibleCount;

    if (loading) {
        return <div className="comment-section">댓글 불러오는 중...</div>;
    }

    return (
        <div className="comment-section" id="comment-section">
            {authenticated && (
                <div className="comment-form">
                    <form onSubmit={submitComment}>
                        <textarea
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            placeholder="댓글을 입력해주세요."
                            required
                        />
                        <button
                            type="submit"
                            className="comment-submit-btn"
                            disabled={submitting}
                        >
                            등록
                        </button>
                        <div style={{ clear: "both" }} />
                    </form>
                </div>
            )}

            <div className="comment-count-header" style={{ marginBottom: "10px", fontWeight: "bold" }}>
                댓글 <span id="total-comment-count">{comments.length}</span>개
            </div>

            <div className="comment-list">
                {comments.length === 0 ? (
                    <div className="no-comment">
                        <p>아직 작성된 댓글이 없습니다.</p>
                        <p>첫 번째 댓글을 남겨보세요!</p>
                    </div>
                ) : (
                    visibleComments.map((comment) => (
                        <div key={comment.id} className="comment-item">
                            <div className="comment-info">
                                <div className="info-left">
                                    <strong>{comment.nickname}</strong>
                                    <small>{formatDate(comment.createdAt)}</small>
                                </div>

                                {comment.editable && (
                                    <div className="comment-actions">
                                        <div className="custom-dropdown">
                                            <button
                                                type="button"
                                                className="drop-btn"
                                                onClick={(e) => {
                                                    const menu = e.currentTarget.nextElementSibling;
                                                    document.querySelectorAll(".drop-content").forEach((el) => {
                                                        if (el !== menu) el.classList.remove("show-menu");
                                                    });
                                                    menu.classList.toggle("show-menu");
                                                }}
                                            >
                                                ⋮
                                            </button>

                                            <div className="drop-content">
                                                <button
                                                    type="button"
                                                    className="menu-item"
                                                    onClick={() => startEdit(comment)}
                                                >
                                                    수정
                                                </button>

                                                <button
                                                    type="button"
                                                    className="delete-btn"
                                                    onClick={() => deleteComment(comment.id)}
                                                >
                                                    삭제
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {editingId === comment.id ? (
                                <div>
                                    <textarea
                                        name="content"
                                        className="edit-textarea"
                                        value={editContent}
                                        onChange={(e) => setEditContent(e.target.value)}
                                    />
                                    <div className="edit-button-group">
                                        <button
                                            type="button"
                                            className="btn-cancel-edit"
                                            onClick={cancelEdit}
                                        >
                                            취소
                                        </button>
                                        <button
                                            type="button"
                                            className="btn-save-edit"
                                            onClick={() => saveEdit(comment.id)}
                                            disabled={submitting}
                                        >
                                            저장
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <div className="comment-content">
                                    <p>{comment.content}</p>
                                </div>
                            )}
                        </div>
                    ))
                )}

                {hasMore && (
                    <div className="more-btn-container">
                        <button
                            type="button"
                            className="btn-more"
                            onClick={() => setVisibleCount((prev) => prev + 10)}
                        >
                            + 더보기
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}