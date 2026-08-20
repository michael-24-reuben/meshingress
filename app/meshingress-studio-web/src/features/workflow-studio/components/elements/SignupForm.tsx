import { useState, type FormEvent, type ReactElement } from 'react'
import { GoogleIdentityButton } from '../../../../auth/GoogleIdentityButton'

interface SignupFormProps {
  googleClientId: string
  onGoogleCredential: (credential: string) => void
  onGoogleError: (message: string) => void
  onSignIn: () => void
}

function UserIdentityIcon({ size = 16 }: { size?: number }): ReactElement {
  return (
    <svg aria-hidden="true" fill="none" height={size} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width={size}>
      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  )
}

function MailIcon(): ReactElement {
  return (
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width="16">
      <rect height="16" rx="2" width="20" x="2" y="4" />
      <path d="m2 7 10 6L22 7" />
    </svg>
  )
}

function LockIcon(): ReactElement {
  return (
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width="16">
      <rect height="11" rx="2" width="18" x="3" y="11" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  )
}

function EyeIcon({ hidden }: { hidden: boolean }): ReactElement {
  return hidden ? (
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width="16">
      <path d="m3 3 18 18" />
      <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
      <path d="M9.9 4.2A10.7 10.7 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.2 3.2M6.1 6.1A18.5 18.5 0 0 0 1 12s4 8 11 8a10.7 10.7 0 0 0 5.9-1.8" />
    </svg>
  ) : (
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width="16">
      <path d="M2 12s4-8 10-8 10 8 10 8-4 8-10 8S2 12 2 12Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

/**
 * Drawer-sized adaptation of scratch/templates/signin/brutalist-style.tsx.
 * Native account registration is intentionally not implied: the server only
 * exposes a development credential-validation endpoint at present.
 */
export function SignupForm({ googleClientId, onGoogleCredential, onGoogleError, onSignIn }: SignupFormProps) {
  const [showPassword, setShowPassword] = useState(false)
  const [registrationMessage, setRegistrationMessage] = useState('')

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setRegistrationMessage('Native account creation is not available in this Studio build. Use a configured identity provider or the development sign-in check below.')
  }

  return (
    <div className="signup-form">
      <form className="signup-form-fields" onSubmit={submit}>
        <label>
          Full name
          <span className="signup-input-wrap">
            <UserIdentityIcon />
            <input autoComplete="name" name="fullName" placeholder="Enter your full name" required type="text" />
          </span>
        </label>
        <label>
          Email
          <span className="signup-input-wrap">
            <MailIcon />
            <input autoComplete="email" name="email" placeholder="name@example.com" required type="email" />
          </span>
        </label>
        <label>
          Password
          <span className="signup-input-wrap">
            <LockIcon />
            <input autoComplete="new-password" name="password" placeholder="Create a password" required type={showPassword ? 'text' : 'password'} />
            <button aria-label={showPassword ? 'Hide password' : 'Show password'} className="signup-password-visibility" onClick={() => setShowPassword((current) => !current)} type="button">
              <EyeIcon hidden={showPassword} />
            </button>
          </span>
        </label>
        <label className="signup-terms">
          <input required type="checkbox" />
          <span>I agree to the <button type="button">Terms of Service</button> and <button type="button">Privacy Policy</button>.</span>
        </label>
        <button className="signup-submit" type="submit">Create account</button>
      </form>

      {registrationMessage && <p aria-live="polite" className="profile-menu-hint">{registrationMessage}</p>}

      <div className="signup-divider"><span>Or continue with</span></div>
      <GoogleIdentityButton
        clientId={googleClientId}
        type="standard"
        text="signup_with"
        theme="outline"
        onCredential={onGoogleCredential}
        onError={onGoogleError}
      />

      {/* <GitHubIdentityButton /> */}
      <p className="signup-signin-copy">Already have an account? <button onClick={onSignIn} type="button">Sign in</button></p>
    </div>
  )
}
