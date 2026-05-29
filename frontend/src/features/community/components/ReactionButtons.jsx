import { useState } from "react";

/* global apiFetch, showAlertModal */
const apiFetch = window.apiFetch;
const showAlertModal = window.showAlertModal;

export default function ReactionButtons({
                                            postId,
                                            initialLiked,
                                            initialDisliked,
                                            initialLikeCount,
                                            initialDislikeCount,
                                            showDislike,
                                        }) {
    const [liked, setLiked] = useState(initialLiked);
    const [disliked, setDisliked] = useState(initialDisliked);
    const [likeCount, setLikeCount] = useState(initialLikeCount);
    const [dislikeCount, setDislikeCount] = useState(initialDislikeCount);
    const [loading, setLoading] = useState(false);

    const showError = (message) => {
        if (showAlertModal) {
            showAlertModal(message);
        } else {
            alert(message);
        }
    };

    const toggleReaction = async (type) => {
        if (loading) return;

        setLoading(true);

        try {
            const url =
                type === "LIKE"
                    ? `/hometop/api/resident/${postId}/like`
                    : `/hometop/api/resident/${postId}/dislike`;

            const response = await apiFetch(url, {
                method: "POST",
            });

            const data = await response.json();

            if (!response.ok) {
                showError(data.message || "처리 중 오류가 발생했습니다.");
                return;
            }

            setLiked(data.liked);
            setDisliked(data.disliked);
            setLikeCount(data.likeCount);
            setDislikeCount(data.dislikeCount);
        } catch (error) {
            console.error(error);
            showError("서버와 통신 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="reaction-section" style={{ display: "flex", gap: "12px" }}>
            <button
                type="button"
                className={`reaction-btn like-btn ${liked ? "active" : ""}`}
                onClick={() => toggleReaction("LIKE")}
                disabled={loading}
            >
                <i className={liked ? "fa-solid fa-heart" : "fa-regular fa-heart"}></i>
                <span className="like-count">{likeCount}</span>
            </button>

            {showDislike && (
                <button
                    type="button"
                    className={`reaction-btn dislike-btn ${disliked ? "active" : ""}`}
                    onClick={() => toggleReaction("DISLIKE")}
                    disabled={loading}
                >
                    <i className={disliked ? "fa-solid fa-thumbs-down" : "fa-regular fa-thumbs-down"}></i>
                    <span className="dislike-count">{dislikeCount}</span>
                </button>
            )}
        </div>
    );
}