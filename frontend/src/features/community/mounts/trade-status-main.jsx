import { createRoot } from "react-dom/client";
import TradeStatus from "../components/TradeStatus.jsx";

const rootEl = document.getElementById("trade-status-root");

if (rootEl) {
    createRoot(rootEl).render(
        <TradeStatus
            postId={Number(rootEl.dataset.postId)}
            initialStatus={rootEl.dataset.tradeStatus}
        />
    );
}