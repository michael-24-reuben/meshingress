import type { ReactElement, ReactNode, SVGProps } from 'react'

export type WorkflowNodeKind = 'trigger' | 'tool' | (string & {})

export type NodeIconName =
  | 'manual-trigger'
  | 'powershell'
  | 'hello-world'
  | 'toonverse-search'
  | 'tool'

export interface NodeIconProps extends Omit<SVGProps<SVGSVGElement>, 'children' | 'height' | 'width'> {
  nodeKind: WorkflowNodeKind
  toolId?: string
  size?: number
  title?: string
}

export interface CodeSquareFilledIconProps
  extends Omit<SVGProps<SVGSVGElement>, 'children' | 'height' | 'width'> {
  size?: number
  title?: string
}

interface IconFrameProps extends CodeSquareFilledIconProps {
  children: ReactNode
  viewBox?: string
}

function resolveNodeIcon(nodeKind: WorkflowNodeKind, toolId?: string): NodeIconName {
  const kind = nodeKind.toLowerCase()
  const tool = toolId?.toLowerCase() ?? ''

  if (kind === 'trigger' || tool === 'trigger.manual' || tool.includes('manual-trigger')) {
    return 'manual-trigger'
  }
  if (tool.includes('powershell') || tool === 'cli.ps') {
    return 'powershell'
  }
  if (tool.includes('helloworld') || tool.includes('hello-world')) {
    return 'hello-world'
  }
  if (tool.includes('toonverse')) {
    return 'toonverse-search'
  }
  return 'tool'
}

function IconFrame({
  size = 20,
  title,
  'aria-label': ariaLabel,
  'aria-hidden': ariaHidden,
  className,
  children,
  viewBox = '0 0 24 24',
  ...svgProps
}: IconFrameProps): ReactElement {
  const isDecorative = ariaHidden ?? !(ariaLabel || title)

  return (
    <svg
      aria-hidden={isDecorative || undefined}
      aria-label={isDecorative ? undefined : ariaLabel}
      className={className}
      fill="none"
      height={size}
      role={isDecorative ? undefined : 'img'}
      viewBox={viewBox}
      width={size}
      xmlns="http://www.w3.org/2000/svg"
      {...svgProps}
    >
      {title ? <title>{title}</title> : null}
      {children}
    </svg>
  )
}

/** A temporary filled code-square glyph for menus and compact actions. */
export function CodeSquareFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame {...props}>
      <path d="M3.75 3.75h16.5v16.5H3.75z" fill="currentColor" opacity=".18" />
      <path
        d="m9.25 8.25-3 3.75 3 3.75M14.75 8.25l3 3.75-3 3.75M13.25 7.5l-2.5 9"
        stroke="currentColor"
        strokeLinecap="square"
        strokeLinejoin="miter"
        strokeWidth="1.7"
      />
    </IconFrame>
  )
}

export function PencilIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame {...props}>
      <path d="m14.25 5.25 4.5 4.5M4.5 19.5l4-1 10.25-10.25-3.5-3.5L5 15l-.5 4.5Z" stroke="currentColor" strokeLinecap="square" strokeLinejoin="miter" strokeWidth="1.6" />
    </IconFrame>
  )
}

export function LightningIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 2048 2048" {...props}>
      <path d="M0 0h2048v2048H0z" fill="none" />
      <path
        d="M1664 0v128H0V0zm-649 512l-67 128H0V512zM0 1024h747l-67 128H0zm1512 0h568L1004 2048H747l304-640H691l535-1024h612zm-559 896l807-768h-456l325-640h-325l-402 768h351l-304 640z"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function ServerIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M18.437 11H5.565a2.5 2.5 0 0 1-2.5-2.5V5.564a2.5 2.5 0 0 1 2.5-2.5h12.872a2.5 2.5 0 0 1 2.5 2.5V8.5a2.5 2.5 0 0 1-2.5 2.5M5.565 4.064a1.5 1.5 0 0 0-1.5 1.5V8.5a1.5 1.5 0 0 0 1.5 1.5h12.872a1.5 1.5 0 0 0 1.5-1.5V5.564a1.5 1.5 0 0 0-1.5-1.5Zm12.872 16.872H5.565a2.5 2.5 0 0 1-2.5-2.5V15.5a2.5 2.5 0 0 1 2.5-2.5h12.872a2.5 2.5 0 0 1 2.5 2.5v2.934a2.5 2.5 0 0 1-2.5 2.502M5.565 14a1.5 1.5 0 0 0-1.5 1.5v2.934a1.5 1.5 0 0 0 1.5 1.5h12.872a1.5 1.5 0 0 0 1.5-1.5V15.5a1.5 1.5 0 0 0-1.5-1.5Z"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function BraceVariableIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 20 20" {...props}>
      <path d="M0 0h20v20H0z" fill="none" />
      <path
        d="M5.75 2.5A2.75 2.75 0 0 0 3 5.25v3.132c0 .397-.222.76-.574.942a.75.75 0 0 0 0 1.352c.352.182.574.545.574.942v3.132a2.75 2.75 0 0 0 2.75 2.75a.75.75 0 0 0 0-1.5c-.69 0-1.25-.56-1.25-1.25v-3.132c0-.6-.21-1.17-.576-1.618A2.56 2.56 0 0 0 4.5 8.382V5.25C4.5 4.56 5.06 4 5.75 4a.75.75 0 0 0 0-1.5m8.5 0A2.75 2.75 0 0 1 17 5.25v3.132c0 .397.222.76.574.942a.75.75 0 0 1 0 1.352a1.06 1.06 0 0 0-.574.942v3.132a2.75 2.75 0 0 1-2.75 2.75a.75.75 0 0 1 0-1.5c.69 0 1.25-.56 1.25-1.25v-3.132c0-.6.21-1.17.576-1.618a2.56 2.56 0 0 1-.576-1.618V5.25c0-.69-.56-1.25-1.25-1.25a.75.75 0 0 1 0-1.5M8.11 6.064a.75.75 0 1 0-1.22.872L9.078 10L6.89 13.064a.75.75 0 0 0 1.22.872L10 11.29l1.89 2.646a.75.75 0 0 0 1.22-.872L10.922 10l2.188-3.064a.75.75 0 0 0-1.22-.872L10 8.71z"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export const StackIcon = ServerIcon
export const ActionIcon = LightningIcon

export function TextObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 512 512" {...props}>
      <path d="M0 0h512v512H0z" fill="none" />
      <path
        d="m292.6 407.78-120-320a22 22 0 0 0-41.2 0l-120 320a22 22 0 0 0 41.2 15.44l36.16-96.42a2 2 0 0 1 1.87-1.3h122.74a2 2 0 0 1 1.87 1.3l36.16 96.42a22 22 0 0 0 41.2-15.44m-185.84-129l43.37-115.65a2 2 0 0 1 3.74 0l43.37 115.67a2 2 0 0 1-1.87 2.7h-86.74a2 2 0 0 1-1.87-2.7ZM400.77 169.5c-41.72-.3-79.08 23.87-95 61.4a22 22 0 0 0 40.5 17.2c8.88-20.89 29.77-34.44 53.32-34.6c32.32-.22 58.41 26.5 58.41 58.85a1.5 1.5 0 0 1-1.45 1.5c-21.92.61-47.92 2.07-71.12 4.8c-54.75 6.44-87.43 36.29-87.43 79.85c0 23.19 8.76 44 24.67 58.68C337.6 430.93 358 438.5 380 438.5c31 0 57.69-8 77.94-23.22h.06a22 22 0 1 0 44 .19v-143c0-56.18-45-102.56-101.23-102.97M380 394.5c-17.53 0-38-9.43-38-36c0-10.67 3.83-18.14 12.43-24.23c8.37-5.93 21.2-10.16 36.14-11.92c21.12-2.49 44.82-3.86 65.14-4.47a2 2 0 0 1 2 2.1C455 370.1 429.46 394.5 380 394.5"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function NumberObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <path
        d="M9 4.75A.75.75 0 0 1 9.75 4h4a.75.75 0 0 1 .53 1.28l-1.89 1.892c.312.076.604.18.867.319c.742.391 1.244 1.063 1.244 2.005c0 .653-.231 1.208-.629 1.627c-.386.408-.894.653-1.408.777c-1.01.243-2.225.063-3.124-.527a.751.751 0 0 1 .822-1.254c.534.35 1.32.474 1.951.322c.306-.073.53-.201.67-.349c.129-.136.218-.32.218-.596c0-.308-.123-.509-.444-.678c-.373-.197-.98-.318-1.806-.318a.75.75 0 0 1-.53-1.28l1.72-1.72H9.75A.75.75 0 0 1 9 4.75m-3.587 5.763c-.35-.05-.77.113-.983.572a.75.75 0 1 1-1.36-.632c.508-1.094 1.589-1.565 2.558-1.425c1 .145 1.872.945 1.872 2.222c0 1.433-1.088 2.192-1.79 2.681c-.308.216-.571.397-.772.573H7a.75.75 0 0 1 0 1.5H3.75a.75.75 0 0 1-.75-.75c0-.69.3-1.211.67-1.61c.348-.372.8-.676 1.15-.92c.8-.56 1.18-.904 1.18-1.474c0-.473-.267-.69-.587-.737M5.604.089A.75.75 0 0 1 6 .75v4.77h.711a.75.75 0 0 1 0 1.5H3.759a.75.75 0 0 1 0-1.5H4.5V2.15l-.334.223a.75.75 0 0 1-.832-1.248l1.5-1a.75.75 0 0 1 .77-.037Z"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function JsonObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        clipRule="evenodd"
        d="M7.835 3.97A1 1 0 0 1 6.865 5c-.41.012-.722.077-.955.17a.87.87 0 0 0-.398.29c-.051.085-.116.263-.116.606V9.23c0 .928-.25 1.782-.84 2.459l-.01.012q-.136.15-.288.281q.159.141.3.306c.592.666.838 1.515.838 2.436v3.231c0 .34.07.514.124.598l.012.019c.062.1.152.183.323.244l.033.013c.23.092.547.158.976.17a1 1 0 0 1-.057 2c-.591-.017-1.147-.11-1.646-.307a2.57 2.57 0 0 1-1.324-1.059c-.322-.5-.441-1.084-.441-1.678v-3.23c0-.568-.147-.9-.337-1.112l-.023-.026c-.18-.214-.53-.438-1.212-.56A1 1 0 0 1 1 12.044v-.132a1 1 0 0 1 .821-.984c.665-.12 1.032-.338 1.233-.558c.198-.231.342-.578.342-1.14V6.067c0-.605.118-1.204.447-1.71l.02-.028a2.86 2.86 0 0 1 1.304-1.015c.5-.2 1.053-.295 1.639-.313a1 1 0 0 1 1.029.97m8.33 0a1 1 0 0 1 1.03-.97c.585.018 1.138.113 1.638.313a2.86 2.86 0 0 1 1.324 1.043c.33.506.447 1.105.447 1.71V9.23c0 .56.144.908.343 1.139c.2.22.567.437 1.232.558a1 1 0 0 1 .821.984v.132a1 1 0 0 1-.824.984c-.682.122-1.033.346-1.212.56l-.023.026c-.19.211-.337.544-.337 1.111v3.231c0 .594-.12 1.179-.44 1.678a2.57 2.57 0 0 1-1.325 1.06c-.499.196-1.055.289-1.646.306a1 1 0 1 1-.057-2c.429-.012.746-.078.976-.17l.029-.011l.004-.002a.58.58 0 0 0 .323-.244l.012-.02c.055-.083.124-.257.124-.597v-3.23c0-.922.246-1.771.839-2.437q.14-.165.3-.306a3 3 0 0 1-.288-.281l-.011-.012c-.59-.677-.84-1.53-.84-2.46V6.067c0-.343-.065-.521-.116-.607a.87.87 0 0 0-.398-.289c-.233-.093-.544-.158-.955-.17a1 1 0 0 1-.97-1.03M9 14a1 1 0 0 1 1 1v1a1 1 0 1 1-2 0v-1a1 1 0 0 1 1-1m3 0a1 1 0 0 1 1 1v1a1 1 0 1 1-2 0v-1a1 1 0 0 1 1-1m3 0a1 1 0 0 1 1 1v1a1 1 0 1 1-2 0v-1a1 1 0 0 1 1-1"
        fill="currentColor"
        fillRule="evenodd"
      />
    </IconFrame>
  )
}

export function ArrayObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M16 20q-.425 0-.712-.288T15 19t.288-.712T16 18h2V6h-2q-.425 0-.712-.288T15 5t.288-.712T16 4h2q.825 0 1.413.588T20 6v12q0 .825-.587 1.413T18 20zM6 20q-.825 0-1.412-.587T4 18V6q0-.825.588-1.412T6 4h2q.425 0 .713.288T9 5t-.288.713T8 6H6v12h2q.425 0 .713.288T9 19t-.288.713T8 20z"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function BooleanObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <path
        d="M13.293 8.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-2-2a1 1 0 1 1 1.414-1.414L9 12.586zm-6-7a1 1 0 1 1 1.414 1.414L6.414 5l2.293 2.293a1 1 0 0 1-1.414 1.414L5 6.414L2.707 8.707a1 1 0 0 1-1.414-1.414L3.586 5L1.293 2.707a1 1 0 1 1 1.414-1.414L5 3.586z"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function DateObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="none">
        <rect height="15" rx="2" stroke="currentColor" strokeWidth="2" width="18" x="3" y="6" />
        <path d="M3 10c0-1.886 0-2.828.586-3.414S5.114 6 7 6h10c1.886 0 2.828 0 3.414.586S21 8.114 21 10z" fill="currentColor" />
        <path d="M7 3v3m10-3v3" stroke="currentColor" strokeLinecap="round" strokeWidth="2" />
        <rect fill="currentColor" height="2" rx=".5" width="4" x="7" y="12" />
        <rect fill="currentColor" height="2" rx=".5" width="4" x="7" y="16" />
        <rect fill="currentColor" height="2" rx=".5" width="4" x="13" y="12" />
        <rect fill="currentColor" height="2" rx=".5" width="4" x="13" y="16" />
      </g>
    </IconFrame>
  )
}

export function BinaryObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <g fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5">
        <rect height="4.5" width="3" x="3.25" y="1.75" />
        <path d="m9.75 6.25h3m-3-4.5h1.5v4" />
        <rect height="4.5" width="3" x="9.75" y="9.75" />
        <path d="m3.25 14.25h3m-3-4.5h1.5v4" />
      </g>
    </IconFrame>
  )
}

export function NullObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 15 15" {...props}>
      <path d="M0 0h15v15H0z" fill="none" />
      <path
        d="M7.5.877c1.648 0 3.155.604 4.315 1.6l.832-.83a.5.5 0 0 1 .707.707l-.832.83a6.623 6.623 0 0 1-9.337 9.337l-.831.833a.5.5 0 0 1-.707-.707l.83-.832A6.623 6.623 0 0 1 7.499.877M3.856 11.85a5.673 5.673 0 0 0 7.991-7.991zM7.5 1.826A5.674 5.674 0 0 0 1.826 7.5a5.65 5.65 0 0 0 1.325 3.642l7.99-7.99a5.65 5.65 0 0 0-3.642-1.325"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function UrlObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 32 32" {...props}>
      <path d="M0 0h32v32H0z" fill="none" />
      <path
        d="M24 21V9h-2v14h8v-2zm-4-6v-4c0-1.103-.897-2-2-2h-6v14h2v-6h1.48l2.335 6h2.145l-2.333-6H18c1.103 0 2-.897 2-2m-6-4h4v4h-4zM8 23H4c-1.103 0-2-.897-2-2V9h2v12h4V9h2v12c0 1.103-.897 2-2 2"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function FileObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        clipRule="evenodd"
        d="M14 22h-4c-3.771 0-5.657 0-6.828-1.172S2 17.771 2 14v-4c0-3.771 0-5.657 1.172-6.828S6.239 2 10.03 2c.606 0 1.091 0 1.5.017q-.02.12-.02.244l-.01 2.834c0 1.097 0 2.067.105 2.848c.114.847.375 1.694 1.067 2.386c.69.69 1.538.952 2.385 1.066c.781.105 1.751.105 2.848.105h4.052c.043.534.043 1.19.043 2.063V14c0 3.771 0 5.657-1.172 6.828S17.771 22 14 22"
        fill="currentColor"
        fillRule="evenodd"
      />
      <path
        d="m19.352 7.617l-3.96-3.563c-1.127-1.015-1.69-1.523-2.383-1.788L13 5c0 2.357 0 3.536.732 4.268S15.643 10 18 10h3.58c-.362-.704-1.012-1.288-2.228-2.383"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function UnknownObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 32 32" {...props}>
      <path d="M0 0h32v32H0z" fill="none" />
      <path
        d="M29.391 14.527L17.473 2.609C17.067 2.203 16.533 2 16 2s-1.067.203-1.473.609L2.609 14.527C2.203 14.933 2 15.466 2 16s.203 1.067.609 1.473L14.526 29.39c.407.407.941.61 1.474.61s1.067-.203 1.473-.609L29.39 17.474c.407-.407.61-.94.61-1.474s-.203-1.067-.609-1.473M16 24a1.5 1.5 0 1 1 0-3a1.5 1.5 0 0 1 0 3m1.125-6.752v1.877h-2.25V15H17c1.034 0 1.875-.841 1.875-1.875S18.034 11.25 17 11.25h-2a1.877 1.877 0 0 0-1.875 1.875v.5h-2.25v-.5A4.13 4.13 0 0 1 15 9h2a4.13 4.13 0 0 1 4.125 4.125a4.13 4.13 0 0 1-4 4.123"
        fill="currentColor"
      />
      <path
        d="M16 21a1.5 1.5 0 1 1-.001 3.001A1.5 1.5 0 0 1 16 21m1.125-3.752a4.13 4.13 0 0 0 4-4.123A4.13 4.13 0 0 0 17 9h-2a4.13 4.13 0 0 0-4.125 4.125v.5h2.25v-.5c0-1.034.841-1.875 1.875-1.875h2c1.034 0 1.875.841 1.875 1.875S18.034 15 17 15h-2.125v4.125h2.25z"
        fill="none"
      />
    </IconFrame>
  )
}

export function SecretObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M19.075 21.9L17.5 20.35q-1.225.8-2.613 1.225T12 22q-2.075 0-3.9-.788t-3.175-2.137T2.788 15.9T2 12q0-1.5.425-2.887T3.65 6.5L2.075 4.925q-.3-.3-.3-.712t.3-.713t.713-.3t.712.3l17 17q.3.3.3.7t-.3.7t-.712.3t-.713-.3m.1-5.575l-11.5-11.5q-.45-.45-.362-1.075t.662-.9q.95-.425 1.963-.638T12 2q2.075 0 3.9.788t3.175 2.137T21.213 8.1T22 12q0 1.05-.213 2.063t-.637 1.962q-.275.575-.887.675t-1.088-.375"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function EnumObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M20.894 13.553a1 1 0 0 1-.447 1.341l-8 4a1 1 0 0 1-.894 0l-8-4a1 1 0 0 1 .894-1.788L12 16.88l7.554-3.775a1 1 0 0 1 1.341.447M12.008 5q.056 0 .111.007l.111.02l.086.024l.012.006l.012.002l.029.014l.05.019l.016.009l.012.005l8 4a1 1 0 0 1 0 1.788l-8 4a1 1 0 0 1-.894 0l-8-4a1 1 0 0 1 0-1.788l8-4l.011-.005l.018-.01l.078-.032l.011-.002l.013-.006l.086-.024l.11-.02l.056-.005z"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function CodeObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="m16 18l6-6l-6-6M8 6l-6 6l6 6"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </IconFrame>
  )
}

export function EmailObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="none" fillRule="evenodd">
        <path d="m12.593 23.258l-.011.002l-.071.035l-.02.004l-.014-.004l-.071-.035q-.016-.005-.024.005l-.004.01l-.017.428l.005.02l.01.013l.104.074l.015.004l.012-.004l.104-.074l.012-.016l.004-.017l-.017-.427q-.004-.016-.017-.018m.265-.113l-.013.002l-.185.093l-.01.01-.003.011l.018.43l.005.012l.008.007l.201.093q.019.005.029-.008l.004-.014l-.034-.614q-.005-.018-.02-.022m-.715.002a.02.02 0 0 0-.027.006l-.006.014l-.034.614q.001.018.017.024l.015-.002l.201-.093l.01-.008l.004-.011l.017-.43l-.003-.012l-.01-.01z" />
        <path
          d="M4 12a8 8 0 0 1 8-8c5.367 0 8.445 4.445 8.006 8.39c-.12 1.086-.438 1.723-.72 2.095s-.571.54-.733.62c-.753.377-1.133.212-1.283.093c-.19-.15-.372-.503-.284-1.034l.006-.032l.503-5.032a1 1 0 0 0-1.867-.59A5.03 5.03 0 0 0 12.03 7C9.279 7 7 9.229 7 12c0 2.774 2.288 5 5.038 5c1.212 0 2.35-.436 3.237-1.176c.175.36.425.682.753.942c.917.726 2.172.752 3.42.128c.337-.168.91-.51 1.434-1.203s.956-1.682 1.112-3.08C22.556 7.554 18.633 2 12 2C6.477 2 2 6.477 2 12s4.477 10 10 10a9.96 9.96 0 0 0 4.445-1.04a1 1 0 0 0-.89-1.791A8 8 0 0 1 4 12m5 0c0-1.647 1.364-3 3.03-3c1.92 0 3.364 1.767 2.974 3.62c-.291 1.378-1.539 2.38-2.966 2.38C10.368 15 9 13.645 9 12"
          fill="currentColor"
        />
      </g>
    </IconFrame>
  )
}

export function DecimalObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M17 8a2 2 0 0 1 2 2v4a2 2 0 1 1-4 0v-4a2 2 0 0 1 2-2m-7 0a2 2 0 0 1 2 2v4a2 2 0 1 1-4 0v-4a2 2 0 0 1 2-2m-5 8h.01"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </IconFrame>
  )
}

export function ImageObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        clipRule="evenodd"
        d="M5.75 3h12.5A2.755 2.755 0 0 1 21 5.75v12.5A2.755 2.755 0 0 1 18.25 21H5.75A2.755 2.755 0 0 1 3 18.25V5.75A2.755 2.755 0 0 1 5.75 3m12.5 1.5H5.75c-.69 0-1.25.56-1.25 1.25v11.19l3.865-3.865a1.26 1.26 0 0 1 1.77 0L11.5 14.44l4.115-4.115a1.26 1.26 0 0 1 1.77 0L19.5 12.44V5.75c0-.69-.56-1.25-1.25-1.25m-7.75 4a2 2 0 1 1-4 0a2 2 0 0 1 4 0"
        fill="currentColor"
        fillRule="evenodd"
      />
    </IconFrame>
  )
}

export function TimeObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v3.764a2 2 0 0 0 1.106 1.789L16 14" />
      </g>
    </IconFrame>
  )
}

export function DurationObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <path
        clipRule="evenodd"
        d="M13.25 1a.75.75 0 0 1 0 1.5h-.5v1.822a3.75 3.75 0 0 1-1.386 2.91L10.42 8l.945.768a3.75 3.75 0 0 1 1.386 2.91v1.821h.5a.75.75 0 0 1 0 1.5h-1.23L12 15H4l-.02-.001H2.754a.75.75 0 0 1 0-1.5h.496v-1.821c0-1.13.51-2.198 1.386-2.91L5.579 8l-.943-.768a3.75 3.75 0 0 1-1.386-2.91V2.5h-.5a.75.75 0 0 1 0-1.5zM8.982 8.765a1.9 1.9 0 0 0-1.972.005L5.58 9.932a2.25 2.25 0 0 0-.831 1.746v1.821h6.5v-1.821a2.25 2.25 0 0 0-.831-1.746zM4.75 2.504v1.248h6.5V2.5z"
        fill="currentColor"
        fillRule="evenodd"
      />
    </IconFrame>
  )
}

export function IntervalObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M10 15h2a1.5 1.5 0 0 0 0-3h-2V9h3.5M3 12v.01M21 12v.01M12 21v.01M7.5 4.2v.01m9 15.59v.01m-9-.01v.01M4.2 16.5v.01m15.6-.01v.01m0-9.01v.01M4.2 7.5v.01m12.3-3.304A9.04 9.04 0 0 0 12 3"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </IconFrame>
  )
}

export function ColorObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M3.839 5.858c2.94-3.916 9.03-5.055 13.364-2.36c4.28 2.66 5.854 7.777 4.1 12.577c-1.655 4.533-6.016 6.328-9.159 4.048c-1.177-.854-1.634-1.925-1.854-3.664l-.106-.987l-.045-.398c-.123-.934-.311-1.352-.705-1.572c-.535-.298-.892-.305-1.595-.033l-.351.146l-.179.078c-1.014.44-1.688.595-2.541.416l-.2-.047l-.164-.047c-2.789-.864-3.202-4.647-.565-8.157m.984 6.716l.123.037l.134.03c.439.087.814.015 1.437-.242l.602-.257c1.202-.493 1.985-.54 3.046.05c.917.512 1.275 1.298 1.457 2.66l.053.459l.055.532l.047.422c.172 1.361.485 2.09 1.248 2.644c2.275 1.65 5.534.309 6.87-3.349c1.516-4.152.174-8.514-3.484-10.789c-3.675-2.284-8.899-1.306-11.373 1.987c-2.075 2.763-1.82 5.28-.215 5.816m11.225-1.994a1.25 1.25 0 1 1 2.414-.647a1.25 1.25 0 0 1-2.414.647m.494 3.488a1.25 1.25 0 1 1 2.415-.647a1.25 1.25 0 0 1-2.415.647M14.07 7.577a1.25 1.25 0 1 1 2.415-.647a1.25 1.25 0 0 1-2.415.647m-.028 8.998a1.25 1.25 0 1 1 2.414-.647a1.25 1.25 0 0 1-2.414.647m-3.497-9.97a1.25 1.25 0 1 1 2.415-.646a1.25 1.25 0 0 1-2.415.646"
        fill="currentColor"
      />
    </IconFrame>
  )
}

export function LocationObjectTypeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M21.45 11.227h-1.39a8.18 8.18 0 0 0-7.36-7.36v-1.39a.75.75 0 0 0-1.5 0v1.39a8.17 8.17 0 0 0-7.31 7.36H2.5a.75.75 0 1 0 0 1.5h1.39a8.18 8.18 0 0 0 7.36 7.36v1.39a.75.75 0 0 0 1.5 0v-1.39a8.19 8.19 0 0 0 7.36-7.36h1.39a.75.75 0 1 0 0-1.5zm-9.5 7.39a6.64 6.64 0 1 1 6.64-6.64a6.65 6.65 0 0 1-6.64 6.65z"
        fill="currentColor"
      />
      <path d="M16.48 11.987a4.54 4.54 0 1 1-4.53-4.54a4.53 4.53 0 0 1 4.53 4.54" fill="currentColor" />
    </IconFrame>
  )
}

export const TextObjectType = TextObjectTypeIcon
export const NumberObjectType = NumberObjectTypeIcon
export const JsonObjectType = JsonObjectTypeIcon
export const ArrayObjectType = ArrayObjectTypeIcon
export const BooleanObjectType = BooleanObjectTypeIcon
export const DateObjectType = DateObjectTypeIcon
export const BinaryObjectType = BinaryObjectTypeIcon
export const NullObjectType = NullObjectTypeIcon
export const UrlObjectType = UrlObjectTypeIcon
export const FileObjectType = FileObjectTypeIcon
export const UnknownObjectType = UnknownObjectTypeIcon
export const SecretObjectType = SecretObjectTypeIcon
export const EnumObjectType = EnumObjectTypeIcon
export const CodeObjectType = CodeObjectTypeIcon
export const EmailObjectType = EmailObjectTypeIcon
export const DecimalObjectType = DecimalObjectTypeIcon
export const ImageObjectType = ImageObjectTypeIcon
export const ClockObjectType = TimeObjectTypeIcon
export const TimeObjectType = TimeObjectTypeIcon
export const DurationObjectType = DurationObjectTypeIcon
export const IntervalObjectType = IntervalObjectTypeIcon
export const ColorObjectType = ColorObjectTypeIcon
export const LocationObjectType = LocationObjectTypeIcon
export const GeoObjectType = LocationObjectTypeIcon


/**
 * Selects a recognizable SVG glyph from a workflow node's generic kind and tool id.
 * Icons are decorative by default; supply `title` or `aria-label` when it conveys
 * information not already available in adjacent node text.
 */
export function NodeIcon({ nodeKind, toolId, ...svgProps }: NodeIconProps): ReactElement {
  const icon = resolveNodeIcon(nodeKind, toolId)

  switch (icon) {
    case 'manual-trigger':
      return (
        <IconFrame {...svgProps}>
          <path d="M7 4.5 18.5 12 7 19.5V4.5Z" fill="currentColor" />
          <path d="M4.5 7.25v9.5" stroke="currentColor" strokeLinecap="square" strokeWidth="1.5" />
        </IconFrame>
      )
    case 'powershell':
      return (
        <IconFrame {...svgProps}>
          <path d="M3.5 5.25h17v13.5h-17z" stroke="currentColor" strokeWidth="1.5" />
          <path d="m7 9 3 3-3 3m5 0h5" stroke="currentColor" strokeLinecap="square" strokeWidth="1.7" />
        </IconFrame>
      )
    case 'hello-world':
      return (
        <IconFrame {...svgProps}>
          <path d="M4 5.25h16v10.5H8.25L4 19V5.25Z" stroke="currentColor" strokeLinejoin="miter" strokeWidth="1.5" />
          <path d="M8 10.25v-2m3.5 2v-2m3.5 2v-2M8 13.5h8" stroke="currentColor" strokeLinecap="square" strokeWidth="1.5" />
        </IconFrame>
      )
    case 'toonverse-search':
      return (
        <IconFrame {...svgProps}>
          <circle cx="10.5" cy="10.5" r="5.75" stroke="currentColor" strokeWidth="1.5" />
          <path d="m15 15 4.5 4.5M8.25 10.5h4.5M10.5 8.25v4.5" stroke="currentColor" strokeLinecap="square" strokeWidth="1.5" />
        </IconFrame>
      )
    default:
      return (
        <IconFrame {...svgProps}>
          <path d="M5 5h14v14H5z" stroke="currentColor" strokeWidth="1.5" />
          <path d="m8.25 9 3 3-3 3m5 0h2.5" stroke="currentColor" strokeLinecap="square" strokeWidth="1.7" />
        </IconFrame>
      )
  }
}

