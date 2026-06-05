function Footer() {
  return (
    <footer className="footer">
      <span style={{ flex: 1 }}></span>
      <span style={{ flex: 1, textAlign: 'center' }}>© 2026 Home Talk One Pass. All rights reserved.</span>
      <span style={{ flex: 1, textAlign: 'right', paddingRight: '24px', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '8px' }}>
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128" width="20px" height="20px">
          <path fill="#fff" d="M98.8 60.9L102.6 66.8 98.8 66.8 98.8 111 29.2 111 29.2 67.9 23.8 66.3 29.2 60.9 64 24.8z"/>
          <path fill="#c7d7e2" d="M40.3 111L40.3 71.8 59.6 71.8 59.6 111M64 17L17.6 64 23.5 69.9 64 27.8 104.5 69.9 110.4 64 98.8 52.3 98.8 24.8 87.2 24.8 87.2 40.5z"/>
        </svg>
        <span style={{ fontSize: '11px', color: '#aaa', fontWeight: '600' }}>HomeTalk OnePass</span>
      </span>
    </footer>
  )
}

export default Footer