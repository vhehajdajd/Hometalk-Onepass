import { useState } from 'react'

// Spring Boot context-path와 맞춘 API prefix.
const CONTEXT_PATH = '/hometop'

// Daum 우편번호 스크립트는 사용자가 주소찾기를 실행할 때만 동적으로 로드한다.
// 고정 id를 사용해 같은 스크립트가 여러 번 삽입되지 않게 한다.
const POSTCODE_SCRIPT_ID = 'daum-postcode-script'
const POSTCODE_SCRIPT_SRC = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'

// 회원가입 폼의 초기값이다.
// input name과 객체 key를 맞춰 공통 변경 핸들러에서 값을 갱신한다.
const initialForm = {
  email: '',
  loginId: '',
  password: '',
  passwordConfirm: '',
  name: '',
  nickname: '',
  phoneNumber: '',
  postNum: '',
  buildingName: '',
  dong: '',
  ho: '',
}

function loadPostcodeScript() {
  // 이미 API가 로드되어 있으면 추가 script 태그를 만들지 않는다.
  if (window.daum?.Postcode || window.kakao?.Postcode) {
    return Promise.resolve()
  }

  // script 태그는 있지만 아직 로딩 중일 수 있으므로 기존 태그의 이벤트를 기다린다.
  const existingScript = document.getElementById(POSTCODE_SCRIPT_ID)
  if (existingScript) {
    return new Promise((resolve, reject) => {
      existingScript.addEventListener('load', resolve, { once: true })
      existingScript.addEventListener('error', reject, { once: true })
    })
  }

  // 최초 호출 시 script 태그를 만들고, 호출부가 await할 수 있도록 Promise로 감싼다.
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.id = POSTCODE_SCRIPT_ID
    script.src = POSTCODE_SCRIPT_SRC
    script.onload = resolve
    script.onerror = reject
    document.body.appendChild(script)
  })
}

function RegisterPage() {
  // step 1은 회원 기본 정보, step 2는 세대 주소 정보 입력 화면이다.
  const [step, setStep] = useState(1)
  // 모든 입력값은 React state로 관리하는 controlled input이다.
  const [form, setForm] = useState(initialForm)
  // 클라이언트 검증 실패와 서버 실패 메시지를 같은 위치에 표시한다.
  const [errorMessage, setErrorMessage] = useState('')
  // 제출 중 버튼 비활성화와 중복 제출 방지를 위한 상태다.
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (event) => {
    const { name, value } = event.target
    // input name을 form key로 사용해 여러 필드를 하나의 핸들러로 갱신한다.
    setForm((current) => ({
      ...current,
      [name]: value,
    }))
  }

  const updateAddress = (address) => {
    // 우편번호 API에서 받은 주소 일부만 기존 form 상태에 병합한다.
    setForm((current) => ({
      ...current,
      ...address,
    }))
  }

  const openAddressSearch = async () => {
    setErrorMessage('')

    try {
      // 기존 Thymeleaf 회원가입 화면에서 쓰던 Daum 우편번호 API를 React에서 동적으로 로드한다.
      // 주소찾기 버튼을 누른 시점에 외부 우편번호 스크립트를 로드한다.
      await loadPostcodeScript()

      // 환경에 따라 Postcode 생성자가 window.daum 또는 window.kakao 아래에 있을 수 있어 둘 다 확인한다.
      const Postcode = window.daum?.Postcode || window.kakao?.Postcode
      if (!Postcode) {
        throw new Error('주소찾기 API를 불러오지 못했습니다.')
      }

      new Postcode({
        oncomplete: (data) => {
          // 사용자가 선택한 주소 타입에 따라 도로명 주소 또는 지번 주소를 기본 주소로 사용한다.
          const baseAddress = data.userSelectedType === 'R'
            ? data.roadAddress
            : data.jibunAddress
          const extraAddressParts = []

          if (data.bname && /[동로가]$/.test(data.bname)) {
            extraAddressParts.push(data.bname)
          }

          if (data.buildingName && data.apartment === 'Y') {
            extraAddressParts.push(data.buildingName)
          }

          const extraAddress = extraAddressParts.length > 0
            ? ` (${extraAddressParts.join(', ')})`
            : ''

          // 우편번호와 기본 주소만 자동 입력하고, 동/호수는 사용자가 직접 입력하게 둔다.
          updateAddress({
            postNum: data.zonecode || '',
            buildingName: `${baseAddress}${extraAddress}`,
          })
        },
      }).open()
    } catch (error) {
      setErrorMessage(error.message || '주소찾기 API 실행에 실패했습니다.')
    }
  }

  const goNextStep = () => {
    setErrorMessage('')

    // passwordConfirm은 화면 검증용 필드라 서버로 보내지 않는다.
    // 다음 단계로 넘어가기 전에 프론트에서 먼저 비밀번호 일치 여부를 확인한다.
    if (form.password !== form.passwordConfirm) {
      setErrorMessage('비밀번호가 일치하지 않습니다.')
      return
    }

    setStep(2)
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setErrorMessage('')
    setIsSubmitting(true)

    try {
      // TODO: 백엔드에 /api/auth/register가 준비되면 이 요청을 실제 회원가입 API로 연결한다.
      // 백엔드 RegisterApiController는 JSON 요청 본문을 SignUpDTO로 받는다.
      // /hometop/api/auth/register 요청은 Vite proxy를 통해 localhost:8090 백엔드로 전달된다.
      const response = await fetch(`${CONTEXT_PATH}/api/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        // key 이름은 SignUpDTO 필드명과 맞춰야 Jackson 역직렬화가 정상 동작한다.
        body: JSON.stringify({
          email: form.email,
          loginId: form.loginId,
          password: form.password,
          name: form.name,
          nickname: form.nickname,
          phoneNumber: form.phoneNumber,
          postNum: form.postNum,
          buildingName: form.buildingName,
          dong: form.dong,
          ho: form.ho,
        }),
      })

      // 성공/실패 응답 모두 JSON message를 내려줄 수 있다.
      // 보안 필터 오류처럼 body가 비어 있을 수 있어 파싱 실패는 빈 객체로 처리한다.
      const data = await response.json().catch(() => ({}))

      // 서버가 내려준 검증 메시지가 있으면 우선 표시한다.
      if (!response.ok) {
        throw new Error(data.message || '회원가입에 실패했습니다.')
      }

      // 회원가입 성공 후 로그인 URL로 이동해 App.jsx가 LoginPage를 다시 렌더링하게 한다.
      window.location.href = `${CONTEXT_PATH}/auth`
    } catch (error) {
      // 네트워크 오류 또는 서버 검증 실패 메시지를 화면 에러 영역에 표시한다.
      setErrorMessage(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-panel register-panel" aria-labelledby="register-title">
        <div className="login-brand">
          <div>
            <p className="brand-name">HomeTalk OnePass</p>
            <h1 id="register-title">회원가입</h1>
            <p>{step === 1 ? '회원 정보를 입력하세요.' : '세대 주소 정보를 입력하세요.'}</p>
          </div>
        </div>

        <div className="register-step" aria-label="회원가입 단계">
          <span className={step === 1 ? 'active' : ''}>Step 1 - Profile</span>
          <span className={step === 2 ? 'active' : ''}>Step 2 - Address</span>
        </div>

        {step === 1 ? (
          <form className="login-form" onSubmit={(event) => {
            event.preventDefault()
            goNextStep()
          }}>
            <label className="login-field">
              <span>이메일</span>
              <input
                name="email"
                type="email"
                value={form.email}
                onChange={updateField}
                placeholder="example@hometop.com"
                required
              />
            </label>

            <label className="login-field">
              <span>아이디</span>
              <input
                name="loginId"
                value={form.loginId}
                onChange={updateField}
                placeholder="아이디를 입력하세요"
                required
              />
            </label>

            <label className="login-field">
              <span>비밀번호</span>
              <input
                name="password"
                type="password"
                value={form.password}
                onChange={updateField}
                placeholder="비밀번호를 입력하세요"
                required
              />
            </label>

            <label className="login-field">
              <span>비밀번호 확인</span>
              <input
                name="passwordConfirm"
                type="password"
                value={form.passwordConfirm}
                onChange={updateField}
                placeholder="비밀번호를 다시 입력하세요"
                required
              />
            </label>

            <div className="register-grid">
              <label className="login-field">
                <span>이름</span>
                <input
                  name="name"
                  value={form.name}
                  onChange={updateField}
                  placeholder="이름"
                  required
                />
              </label>

              <label className="login-field">
                <span>별명</span>
                <input
                  name="nickname"
                  value={form.nickname}
                  onChange={updateField}
                  placeholder="별명"
                  required
                />
              </label>
            </div>

            <label className="login-field">
              <span>연락처</span>
              <input
                name="phoneNumber"
                value={form.phoneNumber}
                onChange={updateField}
                placeholder="휴대폰 번호를 입력하세요"
                required
              />
            </label>

            {errorMessage ? <p className="login-message">{errorMessage}</p> : null}

            <button className="login-submit" type="submit">
              다음
            </button>
          </form>
        ) : (
          <form className="login-form" onSubmit={handleSubmit}>
            <label className="login-field">
              <span>우편번호</span>
              <div className="address-row">
                <input
                  name="postNum"
                  value={form.postNum}
                  onChange={updateField}
                  placeholder="우편번호"
                  required
                />
                <button className="address-search-btn" type="button" onClick={openAddressSearch}>
                  주소찾기
                </button>
              </div>
            </label>

            <label className="login-field">
              <span>주소</span>
              <input
                name="buildingName"
                value={form.buildingName}
                onChange={updateField}
                placeholder="주소를 입력하세요"
                required
              />
            </label>

            <div className="register-grid">
              <label className="login-field">
                <span>동</span>
                <input
                  name="dong"
                  value={form.dong}
                  onChange={updateField}
                  placeholder="예: 102"
                  required
                />
              </label>

              <label className="login-field">
                <span>호</span>
                <input
                  name="ho"
                  value={form.ho}
                  onChange={updateField}
                  placeholder="예: 201"
                  required
                />
              </label>
            </div>

            {errorMessage ? <p className="login-message">{errorMessage}</p> : null}

            <div className="register-actions">
              <button className="secondary-submit" type="button" onClick={() => setStep(1)}>
                이전
              </button>
              <button className="login-submit" type="submit" disabled={isSubmitting}>
                {isSubmitting ? '가입 중' : '회원가입'}
              </button>
            </div>
          </form>
        )}

        <p className="signup-line">
          이미 계정이 있으신가요? <a href={`${CONTEXT_PATH}/auth`}>로그인</a>
        </p>
      </section>
    </main>
  )
}

export default RegisterPage
