import { useState } from "react";

/* global apiFetch, showAlertModal */
const apiFetch = window.apiFetch;
const showAlertModal = window.showAlertModal;

export default function TradeStatus({ postId, initialStatus }) {
    const [tradeStatus, setTradeStatus] = useState(initialStatus);
    const [loading, setLoading] = useState(false);

    const showMessage = (message) => {
        if (showAlertModal) {
            showAlertModal(message);
        } else {
            alert(message);
        }
    };

    const updateTradeBadge = (status) => {
        const badge = document.querySelector(".status-badge-inline");

        if (!badge) return;

        const labelMap = {
            SELLING: "거래중",
            RESERVED: "예약중",
            COMPLETED: "완료",
        };

        badge.textContent = labelMap[status] || status;

        badge.classList.remove(
            "badge-selling",
            "badge-reserved",
            "badge-completed"
        );

        badge.classList.add(`badge-${status.toLowerCase()}`);
    };

    const handleChange = async (e) => {
        const nextStatus = e.target.value;
        const prevStatus = tradeStatus;

        setTradeStatus(nextStatus);
        setLoading(true);

        try {
            const response = await apiFetch(
                `/hometop/api/resident/${postId}/trade/status`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        tradeStatus: nextStatus,
                    }),
                }
            );

            if (!response.ok) {
                setTradeStatus(prevStatus);
                showMessage("거래 상태 변경에 실패했습니다.");
                return;
            }

            updateTradeBadge(nextStatus);
            showMessage("거래 상태가 변경되었습니다.");
        } catch (error) {
            console.error(error);
            setTradeStatus(prevStatus);
            showMessage("서버 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <select
            className="status-select"
            value={tradeStatus}
            onChange={handleChange}
            disabled={loading}
        >
            <option value="SELLING">거래중</option>
            <option value="RESERVED">예약중</option>
            <option value="COMPLETED">완료</option>
        </select>
    );
}