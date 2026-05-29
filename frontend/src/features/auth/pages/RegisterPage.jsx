import { useState } from 'react'

// Spring Boot context-path와 맞춘 API prefix.
const CONTEXT_PATH = '/hometop'
const POSTCODE_SCRIPT_ID = 'daum-postcode-script'
const POSTCODE_SCRIPT_SRC = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'

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
  if (window.daum?.Postcode || window.kakao?.Postcode) {
    return Promise.resolve()
  }

  const existingScript = document.getElementById(POSTCODE_SCRIPT_ID)
  if (existingScript) {
    return new Promise((resolve, reject) => {
      existingScript.addEventListener('load', resolve, { once: true })
      existingScript.addEventListener('error', reject, { once: true })
    })
  }

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
  const [step, setStep] = useState(1)
  const [form, setForm] = useState(initialForm)
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({
      ...current,
      [name]: value,
    }))
  }

  const updateAddress = (address) => {
    setForm((current) => ({
      ...current,
      ...address,
    }))
  }

  const openAddressSearch = async () => {
    setErrorMessage('')

    try {
      // 기존 Thymeleaf 회원가입 화면에서 쓰던 Daum 우편번호 API를 React에서 동적으로 로드한다.
      await loadPostcodeScript()

      const Postcode = window.daum?.Postcode || window.kakao?.Postcode
      if (!Postcode) {
        throw new Error('주소찾기 API를 불러오지 못했습니다.')
      }

      new Postcode({
        oncomplete: (data) => {
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
      const response = await fetch(`${CONTEXT_PATH}/api/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
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

      if (!response.ok) {
        throw new Error('회원가입에 실패했습니다.')
      }

      window.location.href = `${CONTEXT_PATH}/auth`
    } catch (error) {
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
            <p>{step === 1 ? '기본 정보를 입력하세요.' : '세대 주소 정보를 입력하세요.'}</p>
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
