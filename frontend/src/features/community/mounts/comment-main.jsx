import { createRoot } from "react-dom/client";
import CommentSection from "../components/CommentSection.jsx";

const rootEl = document.getElementById("comment-root");

if (rootEl) {
    createRoot(rootEl).render(
        <CommentSection
            postId={Number(rootEl.dataset.postId)}
            authenticated={rootEl.dataset.authenticated === "true"}
        />
    );
}