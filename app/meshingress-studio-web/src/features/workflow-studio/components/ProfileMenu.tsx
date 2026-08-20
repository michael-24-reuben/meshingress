import { useEffect, useRef, useState, type ReactElement } from 'react'
import { HollowHexagonIcon } from '../../../components/icons/node-icons'
import { SignupForm } from './elements/SignupForm'

/**
 * Shared interface for user identity profile representation.
 * Inheritable by Google user, Native account, Guest account, etc.
 */
export interface BaseUserProfile {
  id?: string
  displayName: string
  email: string
  avatarUrl?: string
  provider: 'google' | 'native' | 'guest' | string
}

export interface GoogleUserProfile extends BaseUserProfile {
  provider: 'google'
  googleId?: string
  hostedDomain?: string
}

export interface NativeUserProfile extends BaseUserProfile {
  provider: 'native'
  username: string
  roles?: string[]
}

export type StudioProfile = GoogleUserProfile | NativeUserProfile | BaseUserProfile

export function parseUserProfile(profile: StudioProfile | { displayName: string; email: string; provider?: string } | null | undefined): BaseUserProfile {
  if (!profile) {
    return {
      displayName: 'Guest User',
      email: 'guest@meshingress.local',
      provider: 'guest',
    }
  }
  return {
    id: (profile as any).id,
    displayName: profile.displayName || 'Anonymous User',
    email: profile.email || 'user@meshingress.local',
    avatarUrl: (profile as any).avatarUrl,
    provider: profile.provider || 'native',
  }
}

interface NativeValidationResult {
  accepted: boolean
  message: string
  identifierFingerprint: string | null
}

interface ProfileMenuProps {
  googleClientId: string
  profile: StudioProfile | null
  onGoogleCredential: (credential: string) => void
  onGoogleError: (message: string) => void
  onNativeValidate: (username: string, password: string) => Promise<NativeValidationResult>
  nativeValidationAvailable: boolean
  nativeValidationNotice: string
  onSignOut: () => void
}

function SettingsGearIcon({ size = 16 }: { size?: number }): ReactElement {
  return (
    <svg fill="none" height={size} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width={size}>
      <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function UserIdentityIcon({ size = 16 }: { size?: number }): ReactElement {
  return (
    <svg fill="none" height={size} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width={size}>
      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  )
}


function UsersGroupIcon({ size = 16 }: { size?: number }): ReactElement {
  return (
    <svg fill="none" height={size} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width={size}>
      <path d="M17 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M9 21v-2a4 4 0 0 0-4-4H3a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  )
}

function UserPlusIcon({ size = 16 }: { size?: number }): ReactElement {
  return (
    <svg fill="none" height={size} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width={size}>
      <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
      <circle cx="8.5" cy="7" r="4" />
      <line x1="20" x2="20" y1="8" y2="14" />
      <line x1="17" x2="23" y1="11" y2="11" />
    </svg>
  )
}

function EditorSettingsIcon({ size = 16 }: { size?: number }): ReactElement {
  return (
    <svg fill="none" height={size} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width={size}>
      <line x1="4" x2="20" y1="21" y2="21" />
      <line x1="4" x2="20" y1="14" y2="14" />
      <line x1="4" x2="20" y1="7" y2="7" />
      <circle cx="8" cy="14" r="2" />
      <circle cx="16" cy="7" r="2" />
      <circle cx="12" cy="21" r="2" />
    </svg>
  )
}

function PaletteThemeIcon({ size = 16 }: { size?: number }): ReactElement {
  return (
    <svg fill="none" height={size} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width={size}>
      <circle cx="13.5" cy="6.5" r=".5" fill="currentColor" />
      <circle cx="17.5" cy="10.5" r=".5" fill="currentColor" />
      <circle cx="8.5" cy="7.5" r=".5" fill="currentColor" />
      <circle cx="6.5" cy="12.5" r=".5" fill="currentColor" />
      <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.563-2.512 5.563-5.563C22 6.5 17.5 2 12 2z" />
    </svg>
  )
}

function ChevronRightIcon({ className, size = 14 }: { className?: string; size?: number }): ReactElement {
  return (
    <svg className={className} fill="none" height={size} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24" width={size}>
      <polyline points="9 18 15 12 9 6" />
    </svg>
  )
}

export function ProfileMenu({ googleClientId, profile, onGoogleCredential, onGoogleError, onNativeValidate, nativeValidationAvailable, nativeValidationNotice, onSignOut }: ProfileMenuProps) {
  const [open, setOpen] = useState(false)
  const [otherProfilesOpen, setOtherProfilesOpen] = useState(false)
  const [authDrawerOpen, setAuthDrawerOpen] = useState(false)
  const [nativeOpen, setNativeOpen] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [nativeMessage, setNativeMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [avatarFailed, setAvatarFailed] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)

  const parsedProfile = parseUserProfile(profile)

  useEffect(() => {
    setAvatarFailed(false)
  }, [parsedProfile.avatarUrl])

  useEffect(() => {
    const onPointerDown = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false)
        setOtherProfilesOpen(false)
      }
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [])

  const submitNative = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitting(true)
    setNativeMessage('')
    try {
      const result = await onNativeValidate(username, password)
      setNativeMessage(result.accepted
        ? `${result.message} Audit fingerprint: ${result.identifierFingerprint ?? 'unavailable'}.`
        : result.message)
      if (result.accepted) setPassword('')
    } catch (error) {
      setNativeMessage(error instanceof Error ? error.message : 'Native validation failed.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="topbar-controls-container" ref={rootRef}>
      <div className="profile-container-bar">
        {/* Item 1 in topbar right container: Profile Trigger */}
        <button
          aria-expanded={open}
          aria-haspopup="menu"
          className={`profile-trigger ${open ? 'is-active' : ''}`}
          onClick={() => setOpen((current) => !current)}
          type="button"
        >
          <span aria-hidden="true" className="profile-avatar">
            {parsedProfile.displayName.slice(0, 1).toUpperCase()}
          </span>
          <span className="profile-trigger-copy">
            <strong>{parsedProfile.displayName}</strong>
            <small>{profile ? `${parsedProfile.provider} account` : 'Guest'}</small>
          </span>
        </button>

        {/* Item 2 in topbar right container: Settings (icon only) */}
        <button
          aria-expanded={open}
          aria-label="Settings and profile menu"
          className={`profile-settings-btn ${open ? 'is-active' : ''}`}
          onClick={() => setOpen((current) => !current)}
          title="Settings & Profile"
          type="button"
        >
          <SettingsGearIcon size={16} />
        </button>
      </div>

      {open && (
        <section aria-label="Profile and Settings Menu" className="profile-menu-dropdown" role="menu">
          {/* Item 1: [icon] name / email (Not a btn. The slash represents a vertical separator and not an actual char) */}
          <div className="profile-menu-identity-row" role="presentation">
            <span className="profile-menu-identity-icon">
              {parsedProfile.avatarUrl && !avatarFailed
                ? <img alt="" className="profile-menu-identity-image" onError={() => setAvatarFailed(true)} src={parsedProfile.avatarUrl} />
                : <UserIdentityIcon size={34} />}
            </span>
            <div className="profile-menu-identity-info">
              <span className="profile-identity-name">{parsedProfile.displayName}</span>
              {profile && <span className="profile-identity-email">{parsedProfile.email}</span>}
            </div>
            <div className="profile-menu-identity-options">
              {/* Profile settings */}
              <button aria-label="Profile settings" className="profile-menu-identity-option" disabled onClick={(e) => e.preventDefault()} title="Profile settings" type="button">
                <span className="profile-menu-item-icon">
                  <HollowHexagonIcon size={16} />
                </span>
              </button>
            </div>
          </div>

          {/* Item 2: Other Profiles > (with sub-menu items) */}
          <div
            className={`profile-menu-submenu-container ${otherProfilesOpen ? 'is-open' : ''}`}
            onMouseEnter={() => setOtherProfilesOpen(true)}
            onMouseLeave={() => setOtherProfilesOpen(false)}
          >
            <button
              className="profile-menu-item-btn has-submenu"
              onClick={() => setOtherProfilesOpen((prev) => !prev)}
              type="button"
            >
              <div className="profile-menu-item-left">
                <span className="profile-menu-item-icon">
                  <UsersGroupIcon size={16} />
                </span>
                <span>Other Profiles</span>
              </div>
              <ChevronRightIcon className="profile-submenu-caret" size={14} />
            </button>

            {/* Submenu flyout items */}
            {otherProfilesOpen && (
              <div className="profile-submenu-flyout" role="menu">
                <button className="profile-menu-item-btn" disabled onClick={(e) => e.preventDefault()} type="button">
                  <span className="profile-menu-item-icon">
                    <UserPlusIcon size={16} />
                  </span>
                  <span>Add New Profile</span>
                </button>

                <div className="profile-submenu-header">
                  <span>Other profiles</span>
                </div>

                <button className="profile-menu-item-btn" disabled onClick={(e) => e.preventDefault()} type="button">
                  <span className="profile-menu-item-icon">
                    <UserIdentityIcon size={16} />
                  </span>
                  <span>User2</span>
                </button>
              </div>
            )}
          </div>

          {/* Item 4: --- (Separator) */}
          <div className="profile-menu-divider" role="separator" />

          {/* Item 5: [icon] Editor Settings */}
          <button className="profile-menu-item-btn" disabled onClick={(e) => e.preventDefault()} type="button">
            <span className="profile-menu-item-icon">
              <EditorSettingsIcon size={16} />
            </span>
            <span>Editor Settings</span>
          </button>

          {/* Item 6: [icon] Themes */}
          <button className="profile-menu-item-btn" disabled onClick={(e) => e.preventDefault()} type="button">
            <span className="profile-menu-item-icon">
              <PaletteThemeIcon size={16} />
            </span>
            <span>Themes</span>
          </button>

          {/* Secondary / Authentication management footer */}
          <div className="profile-menu-divider" role="separator" />
          {profile ? (
            <button className="profile-menu-signout-btn" onClick={onSignOut} type="button">
              Sign out
            </button>
          ) : (
            <div className="profile-menu-auth-section">
              <button
                className="profile-menu-auth-toggle"
                onClick={() => setAuthDrawerOpen((prev) => !prev)}
                type="button"
              >
                {authDrawerOpen ? 'Hide Sign-in options' : 'Sign in / Accounts'}
              </button>
              {authDrawerOpen && (
                <div className="profile-menu-auth-drawer">
                  <SignupForm
                    googleClientId={googleClientId}
                    onGoogleCredential={onGoogleCredential}
                    onGoogleError={onGoogleError}
                    onSignIn={() => setNativeOpen(true)}
                  />
                  <button className="profile-menu-native-toggle" disabled={!nativeValidationAvailable} onClick={() => setNativeOpen((current) => !current)} type="button">
                    Native sign-in <small>development check</small>
                  </button>
                  {!nativeValidationAvailable && <p className="profile-menu-hint">{nativeValidationNotice}</p>}
                  {nativeOpen && (
                    <form className="native-login-form" onSubmit={(event) => void submitNative(event)}>
                      <label>Username<input autoComplete="username" onChange={(event) => setUsername(event.target.value)} required value={username} /></label>
                      <label>Password<input autoComplete="current-password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} /></label>
                      <button disabled={submitting} type="submit">{submitting ? 'Checking…' : 'Check native login'}</button>
                      {nativeMessage && <p aria-live="polite" className="native-login-result">{nativeMessage}</p>}
                    </form>
                  )}
                </div>
              )}
            </div>
          )}
        </section>
      )}
    </div>
  )
}
