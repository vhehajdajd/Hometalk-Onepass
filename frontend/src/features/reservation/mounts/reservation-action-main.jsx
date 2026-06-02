import React from 'react';
import ReactDOM from 'react-dom/client';
import ReservationAction from '../components/ReservationAction.jsx';

document.querySelectorAll('.reservation-action-root').forEach((el) => {
    const reservationId = el.dataset.id;

    ReactDOM.createRoot(el).render(
        <ReservationAction reservationId={reservationId} />
    );
});