import { useEffect, useRef, useState } from 'react'

export interface GoogleCredentialResponse {
  credential?: string
  select_by?: 'auto' | 'user' | 'user_1tap' | 'user_2tap' | 'btn' | 'btn_confirm' | 'br_btn' | 'metamask'
}

export type GsiTheme = 'outline' | 'filled_blue' | 'filled_black'
export type GsiSize = 'large' | 'medium' | 'small'
export type GsiType = 'standard' | 'icon'
export type GsiText = 'signin_with' | 'signup_with' | 'continue_with' | 'signin'
export type GsiShape = 'rectangular' | 'pill' | 'circle' | 'square'
export type GsiLogoAlignment = 'left' | 'center'
export type GsiContext = 'signin' | 'signup' | 'use'
export type GsiUxMode = 'popup' | 'redirect'

export interface GsiButtonConfiguration {
  type?: GsiType
  theme?: GsiTheme
  size?: GsiSize
  text?: GsiText
  shape?: GsiShape
  logo_alignment?: GsiLogoAlignment
  width?: number
  locale?: string
  click_listener?: () => void
}

export interface IdConfiguration {
  client_id: string
  callback?: (response: GoogleCredentialResponse) => void
  auto_select?: boolean
  cancel_on_tap_outside?: boolean
  prompt_parent_id?: string
  nonce?: string
  context?: GsiContext
  state_cookie_domain?: string
  ux_mode?: GsiUxMode
  login_uri?: string
  allowed_parent_origin?: string | string[]
  intermediate_iframe_close_callback?: () => void
  itp_support?: boolean
}

export interface GoogleIdentityServices {
  accounts: {
    id: {
      initialize: (configuration: IdConfiguration) => void
      renderButton: (element: HTMLElement, options: GsiButtonConfiguration) => void
      cancel: () => void
    }
  }
}

declare global {
  interface Window {
    google?: GoogleIdentityServices
  }
}

let googleScript: Promise<GoogleIdentityServices> | undefined

function loadGoogleIdentityServices(): Promise<GoogleIdentityServices> {
  if (window.google) return Promise.resolve(window.google)
  if (googleScript) return googleScript

  googleScript = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.defer = true
    script.onload = () => (window.google ? resolve(window.google) : reject(new Error('Google Identity Services did not initialize.')))
    script.onerror = () => reject(new Error('Could not load Google Identity Services.'))
    document.head.append(script)
  })
  return googleScript
}

export interface GoogleIdentityButtonProps {
  clientId: string
  onCredential: (credential: string) => void
  onError: (message: string) => void

  // GIS RenderButton Options per official Google Identity reference
  type?: GsiType
  theme?: GsiTheme
  size?: GsiSize
  text?: GsiText
  shape?: GsiShape
  logoAlignment?: GsiLogoAlignment
  width?: number
  locale?: string
  clickListener?: () => void

  // GIS Initialize Options
  autoSelect?: boolean
  cancelOnTapOutside?: boolean
  context?: GsiContext
  uxMode?: GsiUxMode
  loginUri?: string
  nonce?: string
}

export function GoogleIdentityButton({
  clientId,
  onCredential,
  onError,
  type = 'icon',
  theme = 'outline',
  size = 'large',
  text = 'signup_with',
  shape,
  logoAlignment,
  width,
  locale,
  clickListener,
  autoSelect,
  cancelOnTapOutside,
  context,
  uxMode,
  loginUri,
  nonce,
}: GoogleIdentityButtonProps) {
  const targetRef = useRef<HTMLDivElement>(null)
  const onCredentialRef = useRef(onCredential)
  const onErrorRef = useRef(onError)
  const [renderError, setRenderError] = useState('')

  useEffect(() => {
    onCredentialRef.current = onCredential
    onErrorRef.current = onError
  }, [onCredential, onError])

  useEffect(() => {
    if (!clientId) return
    let cancelled = false
    let renderCheck: number | undefined
    setRenderError('')

    void loadGoogleIdentityServices()
      .then((google) => {
        if (cancelled || !targetRef.current) return

        google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => {
            if (!response.credential) {
              onErrorRef.current('Google did not return an identity credential.')
              return
            }
            onCredentialRef.current(response.credential)
          },
          ...(autoSelect !== undefined && { auto_select: autoSelect }),
          ...(cancelOnTapOutside !== undefined && { cancel_on_tap_outside: cancelOnTapOutside }),
          ...(context && { context }),
          ...(uxMode && { ux_mode: uxMode }),
          ...(loginUri && { login_uri: loginUri }),
          ...(nonce && { nonce }),
        })

        targetRef.current.replaceChildren()

        // Per GIS specification:
        // - logo_alignment and text only apply when type is 'standard'
        // - shape defaults to 'square' for icon type, or 'rectangular' for standard type if omitted
        const effectiveShape = shape ?? (type === 'icon' ? 'square' : 'rectangular')

        const buttonConfig: GsiButtonConfiguration = {
          type,
          theme,
          size,
          shape: effectiveShape,
          ...(width !== undefined && { width }),
          ...(locale && { locale }),
          ...(clickListener && { click_listener: clickListener }),
        }

        if (type === 'standard') {
          buttonConfig.text = text
          buttonConfig.logo_alignment = logoAlignment ?? 'left'
        }

        google.accounts.id.renderButton(targetRef.current, buttonConfig)

        renderCheck = window.setTimeout(() => {
          if (cancelled || (targetRef.current?.childElementCount ?? 0) > 0) {
            return
          }
          const message =
            'Google sign-in could not render. Verify this Studio URL is an Authorized JavaScript origin for the configured Google web client, then reload.'
          setRenderError(message)
          onErrorRef.current(message)
        }, 2_000)
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          const message = error instanceof Error ? error.message : 'Could not initialize Google sign-in.'
          setRenderError(message)
          onErrorRef.current(message)
        }
      })

    return () => {
      cancelled = true
      if (renderCheck !== undefined) window.clearTimeout(renderCheck)
      window.google?.accounts.id.cancel()
    }
  }, [
    clientId,
    type,
    theme,
    size,
    text,
    shape,
    logoAlignment,
    width,
    locale,
    clickListener,
    autoSelect,
    cancelOnTapOutside,
    context,
    uxMode,
    loginUri,
    nonce,
  ])

  if (!clientId) {
    return <p className="profile-menu-hint">Google sign-in needs a runtime `googleClientId` configuration.</p>
  }

  return (
    <div className="google-identity-button">
      {/* Google Identity Services owns this subtree. Keep React-rendered status separate so state updates cannot reconcile away the iframe injected by renderButton(). */}
      <div aria-label="Continue with Google" ref={targetRef} />
      {renderError && (
        <p aria-live="polite" className="profile-menu-hint">
          {renderError}
        </p>
      )}
    </div>
  )
}

