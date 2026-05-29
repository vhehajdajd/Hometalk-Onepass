import React from "react";
import { createRoot } from "react-dom/client";
import ReactionButtons from "../components/ReactionButtons.jsx";

const rootEl = document.getElementById("reaction-root");

if (rootEl) {
    createRoot(rootEl).render(
        <ReactionButtons
            postId={Number(rootEl.dataset.postId)}
            initialLiked={rootEl.dataset.liked === "true"}
            initialDisliked={rootEl.dataset.disliked === "true"}
            initialLikeCount={Number(rootEl.dataset.likeCount || 0)}
            initialDislikeCount={Number(rootEl.dataset.dislikeCount || 0)}
            showDislike={rootEl.dataset.showDislike === "true"}
        />
    );
}