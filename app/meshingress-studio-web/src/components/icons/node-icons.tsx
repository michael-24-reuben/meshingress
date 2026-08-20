import { useId, useState, type ReactElement, type ReactNode, type SVGProps } from 'react'

export type BuiltInNodeIconName = 'manual-trigger' | 'meshingress-api' | 'tool' | 'folder' | 'hollow-hexagon'

export type NodeIconDescriptor =
  | { source: 'built-in'; name: BuiltInNodeIconName }
  | { source: 'image'; href: string; alt?: string; fallback: BuiltInNodeIconName }

export interface WorkflowNodeIconProps extends Omit<SVGProps<SVGSVGElement>, 'children' | 'height' | 'width'> {
  descriptor: NodeIconDescriptor
  size?: number
  title?: string
}

export interface CodeSquareFilledIconProps
  extends Omit<SVGProps<SVGSVGElement>, 'children' | 'height' | 'width'> {
  size?: number
  title?: string
}

export interface ColoredIconProps extends CodeSquareFilledIconProps {
  fill?: string
  iconColor?: string
}

export interface IconFrameProps extends CodeSquareFilledIconProps {
  children: ReactNode
  viewBox?: string
}

export function IconFrame({
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
    <IconFrame viewBox="4 4 16 16" {...props}>
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

export function CopyIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 48 48" {...props}>
      <path d="M0 0h48v48H0z" fill="none" />
      <defs>
        <mask id="SVGUpRmCb0d">
          <g fill="none" stroke="#fff" strokeLinejoin="round" strokeWidth="4">
            <path
              d="M13 12.432v-4.62A2.813 2.813 0 0 1 15.813 5h24.374A2.813 2.813 0 0 1 43 7.813v24.375A2.813 2.813 0 0 1 40.188 35h-4.672"
              strokeLinecap="round"
            />
            <path
              d="M32.188 13H7.811A2.813 2.813 0 0 0 5 15.813v24.374A2.813 2.813 0 0 0 7.813 43h24.375A2.813 2.813 0 0 0 35 40.188V15.811A2.813 2.813 0 0 0 32.188 13Z"
              fill="#555"
            />
          </g>
        </mask>
      </defs>
      <path d="M0 0h48v48H0z" fill="currentColor" mask="url(#SVGUpRmCb0d)" />
    </IconFrame>
  )
}

export function GetAsIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M17 16v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h2m3-4H9a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-1m-1 4l-3 3m0 0l-3-3m3 3V3"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </IconFrame>
  )
}

// Data object types icons
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

// Common tools icons
export const Copy = CopyIcon
export const GetAs = GetAsIcon

export function RemoveIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path
        d="M18 6L6 18M6 6l12 12"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </IconFrame>
  )
}
export const Remove = RemoveIcon

export function MinusIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path fill="currentColor" d="M5 13v-2h14v2z" />
    </IconFrame>
  )
}
export const Minus = MinusIcon

export function AddValueIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="currentColor" fillRule="evenodd" clipRule="evenodd">
        <path d="M3 14a1 1 0 0 1 1-1h12a3 3 0 0 0 3-3V6a1 1 0 1 1 2 0v4a5 5 0 0 1-5 5H4a1 1 0 0 1-1-1" />
        <path d="M3.293 14.707a1 1 0 0 1 0-1.414l4-4a1 1 0 0 1 1.414 1.414L5.414 14l3.293 3.293a1 1 0 1 1-1.414 1.414z" />
      </g>
    </IconFrame>
  )
}
export const AddValue = AddValueIcon

// Panel layout icons
export function LeftPanelIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M2 7.25A3.25 3.25 0 0 1 5.25 4h13.5A3.25 3.25 0 0 1 22 7.25v9.5A3.25 3.25 0 0 1 18.75 20H5.25A3.25 3.25 0 0 1 2 16.75zM9.5 5.5v13h9.25a1.75 1.75 0 0 0 1.75-1.75v-9.5a1.75 1.75 0 0 0-1.75-1.75zM8 5.5H5.25A1.75 1.75 0 0 0 3.5 7.25v9.5c0 .966.784 1.75 1.75 1.75H8z"
      />
    </IconFrame>
  )
}
export const LeftPanel = LeftPanelIcon

export function LeftPanelFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M5.25 4A3.25 3.25 0 0 0 2 7.25v9.5A3.25 3.25 0 0 0 5.25 20h13.5A3.25 3.25 0 0 0 22 16.75v-9.5A3.25 3.25 0 0 0 18.75 4zm13.5 1.5c.966 0 1.75.784 1.75 1.75v9.5a1.75 1.75 0 0 1-1.75 1.75H9.5v-13z"
      />
    </IconFrame>
  )
}
export const LeftPanelFilled = LeftPanelFilledIcon

export function RightPanelIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M22 7.25A3.25 3.25 0 0 0 18.75 4H5.25A3.25 3.25 0 0 0 2 7.25v9.5A3.25 3.25 0 0 0 5.25 20h13.5A3.25 3.25 0 0 0 22 16.75zM14.5 5.5v13H5.25a1.75 1.75 0 0 1-1.75-1.75v-9.5c0-.966.784-1.75 1.75-1.75zm1.5 0h2.75c.966 0 1.75.784 1.75 1.75v9.5a1.75 1.75 0 0 1-1.75 1.75H16z"
      />
    </IconFrame>
  )
}
export const RightPanel = RightPanelIcon

export function RightPanelFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M18.75 4A3.25 3.25 0 0 1 22 7.25v9.5A3.25 3.25 0 0 1 18.75 20H5.25A3.25 3.25 0 0 1 2 16.75v-9.5A3.25 3.25 0 0 1 5.25 4zM5.25 5.5A1.75 1.75 0 0 0 3.5 7.25v9.5c0 .966.784 1.75 1.75 1.75h9.25v-13z"
      />
    </IconFrame>
  )
}
export const RightPanelFilled = RightPanelFilledIcon

export function BottomPanelIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M2 18a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v2a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3zm3-2a2 2 0 0 0-2 2v3h14v-3a2 2 0 0 0-2-2zM9 15.5v3"
      />
    </IconFrame>
  )
}
export const BottomPanel = BottomPanelIcon

export function BottomPanelFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M2 6a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3zm3-2a2 2 0 0 0-2 2v5h14V6a2 2 0 0 0-2-2zM9 5.5v5"
      />
    </IconFrame>
  )
}
export const BottomPanelFilled = BottomPanelFilledIcon

export function UiPanelsAllIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <g fill="none">
        <rect height="13" rx="2.5" stroke="currentColor" width="15" x=".5" y="1.5" />
        <path
          d="M2 4.6c0-.56 0-.84.109-1.05c.096-.188.249-.341.437-.437c.214-.109.494-.109 1.05-.109h.4v7h-2v-5.4zM12 3h.4c.56 0 .84 0 1.05.109c.188.096.341.249.437.437c.109.214.109.494.109 1.05v5.4h-2v-7zM2 11h12v.4c0 .56 0 .84-.109 1.05a1 1 0 0 1-.437.437c-.214.109-.494.109-1.05.109h-8.8c-.56 0-.84 0-1.05-.109a1 1 0 0 1-.437-.437c-.109-.214-.109-.494-.109-1.05V11z"
          fill="currentColor"
        />
      </g>
    </IconFrame>
  )
}
export const UiPanelsAll = UiPanelsAllIcon
export const PanelsAllIcon = UiPanelsAllIcon
export const PanelsAll = UiPanelsAllIcon

export function UiPanelsBottomIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <g fill="none">
        <rect height="13" rx="2.5" stroke="currentColor" width="15" x=".5" y="1.5" />
        <path
          d="M2 4.6c0-.56 0-.84.109-1.05c.096-.188.249-.341.437-.437c.214-.109.494-.109 1.05-.109h.4v7h-2v-5.4zM12 3h.4c.56 0 .84 0 1.05.109c.188.096.341.249.437.437c.109.214.109.494.109 1.05v5.4h-2v-7z"
          fill="currentColor"
          opacity=".3"
        />
        <path
          d="M2 11h12v.4c0 .56 0 .84-.109 1.05a1 1 0 0 1-.437.437c-.214.109-.494.109-1.05.109h-8.8c-.56 0-.84 0-1.05-.109a1 1 0 0 1-.437-.437c-.109-.214-.109-.494-.109-1.05V11z"
          fill="currentColor"
        />
      </g>
    </IconFrame>
  )
}
export const UiPanelsBottom = UiPanelsBottomIcon
export const PanelsBottomIcon = UiPanelsBottomIcon
export const PanelsBottom = UiPanelsBottomIcon

export function UiPanelsLeftIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <g fill="none">
        <rect height="13" rx="2.5" stroke="currentColor" width="15" x=".5" y="1.5" />
        <path
          d="M2 4.6c0-.56 0-.84.109-1.05c.096-.188.249-.341.437-.437c.214-.109.494-.109 1.05-.109h.4v7h-2v-5.4z"
          fill="currentColor"
        />
        <path
          d="M12 3h.4c.56 0 .84 0 1.05.109c.188.096.341.249.437.437c.109.214.109.494.109 1.05v5.4h-2v-7zM2 11h12v.4c0 .56 0 .84-.109 1.05a1 1 0 0 1-.437.437c-.214.109-.494.109-1.05.109h-8.8c-.56 0-.84 0-1.05-.109a1 1 0 0 1-.437-.437c-.109-.214-.109-.494-.109-1.05V11z"
          fill="currentColor"
          opacity=".3"
        />
      </g>
    </IconFrame>
  )
}
export const UiPanelsLeft = UiPanelsLeftIcon
export const PanelsLeftIcon = UiPanelsLeftIcon
export const PanelsLeft = UiPanelsLeftIcon

export function UiPanelsLeftBottomIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <g fill="none">
        <rect height="13" rx="2.5" stroke="currentColor" width="15" x=".5" y="1.5" />
        <path
          d="M2 4.6c0-.56 0-.84.109-1.05c.096-.188.249-.341.437-.437c.214-.109.494-.109 1.05-.109h.4v7h-2v-5.4z"
          fill="currentColor"
        />
        <path
          d="M12 3h.4c.56 0 .84 0 1.05.109c.188.096.341.249.437.437c.109.214.109.494.109 1.05v5.4h-2v-7z"
          fill="currentColor"
          opacity=".3"
        />
        <path
          d="M2 11h12v.4c0 .56 0 .84-.109 1.05a1 1 0 0 1-.437.437c-.214.109-.494.109-1.05.109h-8.8c-.56 0-.84 0-1.05-.109a1 1 0 0 1-.437-.437c-.109-.214-.109-.494-.109-1.05V11z"
          fill="currentColor"
        />
      </g>
    </IconFrame>
  )
}
export const UiPanelsLeftBottom = UiPanelsLeftBottomIcon
export const PanelsLeftBottomIcon = UiPanelsLeftBottomIcon
export const PanelsLeftBottom = UiPanelsLeftBottomIcon

export function UiPanelsLeftRightIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <g fill="none">
        <rect height="13" rx="2.5" stroke="currentColor" width="15" x=".5" y="1.5" />
        <path
          d="M2 4.6c0-.56 0-.84.109-1.05c.096-.188.249-.341.437-.437c.214-.109.494-.109 1.05-.109h.4v7h-2v-5.4zM12 3h.4c.56 0 .84 0 1.05.109c.188.096.341.249.437.437c.109.214.109.494.109 1.05v5.4h-2v-7z"
          fill="currentColor"
        />
        <path
          d="M2 11h12v.4c0 .56 0 .84-.109 1.05a1 1 0 0 1-.437.437c-.214.109-.494.109-1.05.109h-8.8c-.56 0-.84 0-1.05-.109a1 1 0 0 1-.437-.437c-.109-.214-.109-.494-.109-1.05V11z"
          fill="currentColor"
          opacity=".3"
        />
      </g>
    </IconFrame>
  )
}
export const UiPanelsLeftRight = UiPanelsLeftRightIcon
export const PanelsLeftRightIcon = UiPanelsLeftRightIcon
export const PanelsLeftRight = UiPanelsLeftRightIcon

export function UiPanelsNoneIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <g fill="none">
        <rect height="13" rx="2.5" stroke="currentColor" width="15" x=".5" y="1.5" />
        <path
          d="M2 4.6c0-.56 0-.84.109-1.05c.096-.188.249-.341.437-.437c.214-.109.494-.109 1.05-.109h.4v7h-2v-5.4zM12 3h.4c.56 0 .84 0 1.05.109c.188.096.341.249.437.437c.109.214.109.494.109 1.05v5.4h-2v-7zM2 11h12v.4c0 .56 0 .84-.109 1.05a1 1 0 0 1-.437.437c-.214.109-.494.109-1.05.109h-8.8c-.56 0-.84 0-1.05-.109a1 1 0 0 1-.437-.437c-.109-.214-.109-.494-.109-1.05V11z"
          fill="currentColor"
          opacity=".3"
        />
      </g>
    </IconFrame>
  )
}
export const UiPanelsNone = UiPanelsNoneIcon
export const PanelsNoneIcon = UiPanelsNoneIcon
export const PanelsNone = UiPanelsNoneIcon

export function UiPanelsRightIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <g fill="none">
        <rect height="13" rx="2.5" stroke="currentColor" width="15" x=".5" y="1.5" />
        <path
          d="M2 4.6c0-.56 0-.84.109-1.05c.096-.188.249-.341.437-.437c.214-.109.494-.109 1.05-.109h.4v7h-2v-5.4z"
          fill="currentColor"
          opacity=".3"
        />
        <path
          d="M12 3h.4c.56 0 .84 0 1.05.109c.188.096.341.249.437.437c.109.214.109.494.109 1.05v5.4h-2v-7z"
          fill="currentColor"
        />
        <path
          d="M2 11h12v.4c0 .56 0 .84-.109 1.05a1 1 0 0 1-.437.437c-.214.109-.494.109-1.05.109h-8.8c-.56 0-.84 0-1.05-.109a1 1 0 0 1-.437-.437c-.109-.214-.109-.494-.109-1.05V11z"
          fill="currentColor"
          opacity=".3"
        />
      </g>
    </IconFrame>
  )
}
export const UiPanelsRight = UiPanelsRightIcon
export const PanelsRightIcon = UiPanelsRightIcon
export const PanelsRight = UiPanelsRightIcon

export function UiPanelsRightBottomIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 16 16" {...props}>
      <path d="M0 0h16v16H0z" fill="none" />
      <g fill="none">
        <rect height="13" rx="2.5" stroke="currentColor" width="15" x=".5" y="1.5" />
        <path
          d="M2 4.6c0-.56 0-.84.109-1.05c.096-.188.249-.341.437-.437c.214-.109.494-.109 1.05-.109h.4v7h-2v-5.4z"
          fill="currentColor"
          opacity=".3"
        />
        <path
          d="M12 3h.4c.56 0 .84 0 1.05.109c.188.096.341.249.437.437c.109.214.109.494.109 1.05v5.4h-2v-7zM2 11h12v.4c0 .56 0 .84-.109 1.05a1 1 0 0 1-.437.437c-.214.109-.494.109-1.05.109h-8.8c-.56 0-.84 0-1.05-.109a1 1 0 0 1-.437-.437c-.109-.214-.109-.494-.109-1.05V11z"
          fill="currentColor"
        />
      </g>
    </IconFrame>
  )
}
export const UiPanelsRightBottom = UiPanelsRightBottomIcon
export const PanelsRightBottomIcon = UiPanelsRightBottomIcon
export const PanelsRightBottom = UiPanelsRightBottomIcon

export function SettingsIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M12.012 2.25c.734.008 1.465.093 2.182.253a.75.75 0 0 1 .582.649l.17 1.527a1.384 1.384 0 0 0 1.927 1.116l1.4-.615a.75.75 0 0 1 .85.174a9.8 9.8 0 0 1 2.205 3.792a.75.75 0 0 1-.272.825l-1.241.916a1.38 1.38 0 0 0 0 2.226l1.243.915a.75.75 0 0 1 .272.826a9.8 9.8 0 0 1-2.204 3.792a.75.75 0 0 1-.849.175l-1.406-.617a1.38 1.38 0 0 0-1.926 1.114l-.17 1.526a.75.75 0 0 1-.571.647a9.5 9.5 0 0 1-4.406 0a.75.75 0 0 1-.572-.647l-.169-1.524a1.382 1.382 0 0 0-1.925-1.11l-1.406.616a.75.75 0 0 1-.85-.175a9.8 9.8 0 0 1-2.203-3.796a.75.75 0 0 1 .272-.826l1.243-.916a1.38 1.38 0 0 0 0-2.226l-1.243-.914a.75.75 0 0 1-.272-.826a9.8 9.8 0 0 1 2.205-3.792a.75.75 0 0 1 .85-.174l1.4.615a1.387 1.387 0 0 0 1.93-1.118l.17-1.526a.75.75 0 0 1 .583-.65q1.074-.238 2.201-.252m0 1.5a9 9 0 0 0-1.354.117l-.11.977A2.886 2.886 0 0 1 6.526 7.17l-.899-.394A8.3 8.3 0 0 0 4.28 9.092l.797.587a2.88 2.88 0 0 1 .001 4.643l-.799.588c.32.842.776 1.626 1.348 2.322l.905-.397a2.882 2.882 0 0 1 4.017 2.318l.109.984c.89.15 1.799.15 2.688 0l.11-.984a2.88 2.88 0 0 1 4.018-2.322l.904.396a8.3 8.3 0 0 0 1.348-2.318l-.798-.588a2.88 2.88 0 0 1-.001-4.643l.797-.587a8.3 8.3 0 0 0-1.348-2.317l-.897.393a2.884 2.884 0 0 1-4.023-2.324l-.109-.976a9 9 0 0 0-1.334-.117M12 8.25a3.75 3.75 0 1 1 0 7.5a3.75 3.75 0 0 1 0-7.5m0 1.5a2.25 2.25 0 1 0 0 4.5a2.25 2.25 0 0 0 0-4.5"
      />
    </IconFrame>
  )
}
export const Settings = SettingsIcon

// Additional UI / Toolbar icons
export function ContextInfoIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const InfoIcon = ContextInfoIcon

export function AboutVariantIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M13.5 4A1.5 1.5 0 0 0 12 5.5A1.5 1.5 0 0 0 13.5 7A1.5 1.5 0 0 0 15 5.5A1.5 1.5 0 0 0 13.5 4m-.36 4.77c-1.19.1-4.44 2.69-4.44 2.69c-.2.15-.14.14.02.42c.16.27.14.29.33.16c.2-.13.53-.34 1.08-.68c2.12-1.36.34 1.78-.57 7.07c-.36 2.62 2 1.27 2.61.87c.6-.39 2.21-1.5 2.37-1.61c.22-.15.06-.27-.11-.52c-.12-.17-.24-.05-.24-.05c-.65.43-1.84 1.33-2 .76c-.19-.57 1.03-4.48 1.7-7.17c.11-.64.41-2.04-.75-1.94"
      />
    </IconFrame>
  )
}
export const AboutVariant = AboutVariantIcon

export function UndoIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Undo = UndoIcon

export function RedoIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.97 7.22l2.37.78c1.05-3.19 4.06-5.5 7.6-5.5 1.96 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Redo = RedoIcon

export function TrashIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M4 5h3V4a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1h3a1 1 0 0 1 0 2h-1v13a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V7H4a1 1 0 1 1 0-2m3 2v13h10V7zm2-2h6V4H9zm0 4h2v9H9zm4 0h2v9h-2z"
      />
    </IconFrame>
  )
}
export const Trash = TrashIcon

export function TrashFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M16 9v10H8V9h8m-1.5-6h-5l-1 1H5v2h14V4h-4.5l-1-1zM18 7H6v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const TrashFilled = TrashFilledIcon

export function FilterIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Filter = FilterIcon

export function TickIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Tick = TickIcon
export const Check = TickIcon

export function CrossIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Cross = CrossIcon
export const Close = CrossIcon

export function FluentWarningIcon({
  fill = '#FFB02E',
  iconColor = '#000',
  ...props
}: ColoredIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 32 32" {...props}>
      <path d="M0 0h32v32H0z" fill="none" />
      <g fill="none">
        <path
          d="m14.839 5.668l-12.66 21.93c-.51.89.13 2.01 1.16 2.01h25.32c1.03 0 1.67-1.11 1.16-2.01l-12.66-21.93c-.52-.89-1.8-.89-2.32 0"
          fill={fill}
        />
        <path
          d="M14.599 21.498a1.4 1.4 0 1 0 2.8-.01v-9.16c0-.77-.62-1.4-1.4-1.4c-.77 0-1.4.62-1.4 1.4zm2.8 3.98a1.4 1.4 0 1 1-2.8 0a1.4 1.4 0 0 1 2.8 0"
          fill={iconColor}
        />
      </g>
    </IconFrame>
  )
}
export const FluentWarning = FluentWarningIcon
export const WarningColoredIcon = FluentWarningIcon
export const WarningIcon = FluentWarningIcon
export const Warning = FluentWarningIcon

export function FluentCriticalIcon({
  fill = '#ff2e2e',
  iconColor = '#000',
  ...props
}: ColoredIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 32 32" {...props}>
      <path d="M0 0h32v32H0z" fill="none" />
      <g fill="none">
        <circle cx="16" cy="16" fill={fill} r="14" />
        <path
          d="M14.599 18.59a1.4 1.4 0 1 0 2.8-.01v-9.16c0-.77-.62-1.4-1.4-1.4c-.77 0-1.4.62-1.4 1.4zm2.8 3.98a1.4 1.4 0 1 1-2.8 0a1.4 1.4 0 0 1 2.8 0"
          fill={iconColor}
        />
      </g>
    </IconFrame>
  )
}
export const FluentCritical = FluentCriticalIcon
export const CriticalColoredIcon = FluentCriticalIcon
export const CriticalIcon = FluentCriticalIcon
export const Critical = FluentCriticalIcon

export function QuestionMarkIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 16h-2v-2h2v2zm1.07-7.75l-.9.92C12.45 11.9 12 12.5 12 14h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H7c0-2.76 2.24-5 5-5s5 2.24 5 5c0 1.04-.42 1.99-1.07 2.75z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const QuestionMark = QuestionMarkIcon
export const Help = QuestionMarkIcon

export function ReloadIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46A7.93 7.93 0 0 0 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74A7.93 7.93 0 0 0 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Reload = ReloadIcon
export const Refresh = ReloadIcon

export function EllipsisHorizontalIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <circle cx="5" cy="12" r="2" fill="currentColor" />
      <circle cx="12" cy="12" r="2" fill="currentColor" />
      <circle cx="19" cy="12" r="2" fill="currentColor" />
    </IconFrame>
  )
}
export const EllipsisHorizontal = EllipsisHorizontalIcon
export const DotsHorizontal = EllipsisHorizontalIcon

export function EllipsisVerticalIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <circle cx="12" cy="5" r="2" fill="currentColor" />
      <circle cx="12" cy="12" r="2" fill="currentColor" />
      <circle cx="12" cy="19" r="2" fill="currentColor" />
    </IconFrame>
  )
}
export const EllipsisVertical = EllipsisVerticalIcon
export const DotsVerticalIcon = EllipsisVerticalIcon
export const DotsVertical = EllipsisVerticalIcon
export const MoreVertical = EllipsisVerticalIcon

export function PlayIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="2 2 20 20" {...props}>
      <path d="M2 2h20v20H2z" fill="none" />
      <path fill="currentColor" d="M7.608 4.615a.75.75 0 0 0-1.108.659v13.452a.75.75 0 0 0 1.108.659l12.362-6.726a.75.75 0 0 0 0-1.318zM5 5.274c0-1.707 1.826-2.792 3.325-1.977l12.362 6.727c1.566.852 1.566 3.1 0 3.952L8.325 20.702C6.826 21.518 5 20.432 5 18.726z" />
    </IconFrame>
  )
}
export const Play = PlayIcon

export function PlayFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="2 2 20 20" {...props}>
      <path d="M2 2h20v20H2z" fill="none" />
      <path fill="currentColor" d="M5 5.274c0-1.707 1.826-2.792 3.325-1.977l12.362 6.727c1.566.852 1.566 3.1 0 3.952L8.325 20.702C6.826 21.518 5 20.432 5 18.726z" />
    </IconFrame>
  )
}
export const PlayFilled = PlayFilledIcon

export function StopIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="2 2 20 20" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path fill="currentColor" d="M20 3H4a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1m-1 16H5V5h14z" />
    </IconFrame>
  )
}
export const Stop = StopIcon

export function StopFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="2 2 20 20" {...props}>
      <path d="M2 2h20v20H2z" fill="none" />
      <path fill="currentColor" d="M4.75 3A1.75 1.75 0 0 0 3 4.75v14.5c0 .966.784 1.75 1.75 1.75h14.5A1.75 1.75 0 0 0 21 19.25V4.75A1.75 1.75 0 0 0 19.25 3z" />
    </IconFrame>
  )
}
export const StopFilled = StopFilledIcon

export function CoffeeLoopIcon(props: CodeSquareFilledIconProps): ReactElement {
  const maskId = useId()
  const gradId = useId()
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <defs>
        <linearGradient id={gradId} x1="0" x2="0" y1="1" y2="8.5" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#fff" stopOpacity="0" />
          <stop offset="30%" stopColor="#fff" stopOpacity="1" />
          <stop offset="100%" stopColor="#fff" stopOpacity="1" />
        </linearGradient>
        <mask id={maskId}>
          <rect fill={`url(#${gradId})`} height="9" width="24" x="0" y="0" />
        </mask>
      </defs>
      <g fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2">
        <path d="M17 9v9c0 1.66 -1.34 3 -3 3h-6c-1.66 0 -3 -1.34 -3 -3v-9Z" />
        <path d="M17 9h3c0.55 0 1 0.45 1 1v3c0 0.55 -0.45 1 -1 1h-3" />
      </g>
      <g mask={`url(#${maskId})`}>
        <g fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.6">
          <path d="M7.5 18c0 -2 1 -2 1 -4s-1 -2 -1 -4s1 -2 1 -4s-1 -2 -1 -4s1 -2 1 -4s-1 -2 -1 -4 M10.5 18c0 -2 1 -2 1 -4s-1 -2 -1 -4s1 -2 1 -4s-1 -2 -1 -4s1 -2 1 -4s-1 -2 -1 -4 M13.5 18c0 -2 1 -2 1 -4s-1 -2 -1 -4s1 -2 1 -4s-1 -2 -1 -4s1 -2 1 -4s-1 -2 -1 -4">
            <animateTransform attributeName="transform" dur="2.4s" repeatCount="indefinite" type="translate" values="0 0; 0 -8" />
          </path>
        </g>
      </g>
    </IconFrame>
  )
}
export const CoffeeLoop = CoffeeLoopIcon
export const CoffeeLoopAnimated = CoffeeLoopIcon
export const PendingCoffeeLoop = CoffeeLoopIcon

export function BouncingBallsIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <circle cx="4" cy="12" r="3" fill="currentColor">
        <animate id="SVGKiXXedfO" attributeName="cy" begin="0;SVGgLulOGrw.end+0.25s" calcMode="spline" dur="0.6s" keySplines=".33,.66,.66,1;.33,0,.66,.33" values="12;6;12" />
      </circle>
      <circle cx="12" cy="12" r="3" fill="currentColor">
        <animate attributeName="cy" begin="SVGKiXXedfO.begin+0.1s" calcMode="spline" dur="0.6s" keySplines=".33,.66,.66,1;.33,0,.66,.33" values="12;6;12" />
      </circle>
      <circle cx="20" cy="12" r="3" fill="currentColor">
        <animate id="SVGgLulOGrw" attributeName="cy" begin="SVGKiXXedfO.begin+0.2s" calcMode="spline" dur="0.6s" keySplines=".33,.66,.66,1;.33,0,.66,.33" values="12;6;12" />
      </circle>
    </IconFrame>
  )
}
export const BouncingBalls = BouncingBallsIcon
export const BouncingBallsAnimated = BouncingBallsIcon
export const PendingBouncing = BouncingBallsIcon

export function GooeyBallsIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <defs>
        <filter id="SVGg4wYRcsm">
          <feGaussianBlur in="SourceGraphic" result="y" stdDeviation="1.5" />
          <feColorMatrix in="y" result="z" values="1 0 0 0 0 0 1 0 0 0 0 0 1 0 0 0 0 0 18 -7" />
          <feBlend in="SourceGraphic" in2="z" />
        </filter>
      </defs>
      <g fill="currentColor" filter="url(#SVGg4wYRcsm)">
        <circle cx="4" cy="12" r="3">
          <animate attributeName="cx" calcMode="spline" dur="1.5s" keySplines=".56,.52,.17,.98;.56,.52,.17,.98" repeatCount="indefinite" values="4;9;4" />
          <animate attributeName="r" calcMode="spline" dur="1.5s" keySplines=".56,.52,.17,.98;.56,.52,.17,.98" repeatCount="indefinite" values="3;8;3" />
        </circle>
        <circle cx="15" cy="12" r="8">
          <animate attributeName="cx" calcMode="spline" dur="1.5s" keySplines=".56,.52,.17,.98;.56,.52,.17,.98" repeatCount="indefinite" values="15;20;15" />
          <animate attributeName="r" calcMode="spline" dur="1.5s" keySplines=".56,.52,.17,.98;.56,.52,.17,.98" repeatCount="indefinite" values="8;3;8" />
        </circle>
      </g>
    </IconFrame>
  )
}
export const GooeyBalls = GooeyBallsIcon
export const GooeyBallsAnimated = GooeyBallsIcon
export const PendingExpanding = GooeyBallsIcon

export function DebugRunIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="m19.854 13.96l-6.643 3.737a2.25 2.25 0 0 0-1.172-1.056l.015-.015l7.063-3.974a.75.75 0 0 0 0-1.307l-12-6.749A.75.75 0 0 0 6 5.25v5.25c-.531 0-1.026.121-1.5.291V5.25c0-1.72 1.853-2.805 3.353-1.96l12 6.75c1.528.859 1.528 3.061 0 3.922zm-9.354 2.1V18h.75a.75.75 0 0 1 0 1.5h-.75c0 .576-.11 1.125-.307 1.632l1.588 1.588a.75.75 0 0 1-1.062 1.061l-1.327-1.328a4.492 4.492 0 0 1-6.783 0L1.28 23.781a.753.753 0 0 1-1.062 0a.75.75 0 0 1 0-1.06l1.589-1.589A4.5 4.5 0 0 1 1.5 19.5H.75a.75.75 0 0 1 0-1.5h.75v-1.94L.219 14.78a.75.75 0 0 1 1.06-1.061L2.562 15H3c0-1.655 1.346-3 3-3c1.655 0 3 1.345 3 3h.44l1.28-1.281a.75.75 0 0 1 1.061 1.06zM4.5 15h3a1.5 1.5 0 0 0-3 0M9 16.5H3v3c0 1.654 1.346 3 3 3c1.655 0 3-1.346 3-3z"
      />
    </IconFrame>
  )
}
export const DebugRun = DebugRunIcon

export function DebugConsoleIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        fill="currentColor"
        d="M5.25 21h6.569l-.659.659a2.2 2.2 0 0 0-.513.841H5.25a3.753 3.753 0 0 1-3.75-3.75V5.25A3.754 3.754 0 0 1 5.25 1.5h13.5a3.754 3.754 0 0 1 3.75 3.75v6.885c-.264.093-.512.23-.726.417a4.5 4.5 0 0 0-.774-.891V5.25C21 4.01 19.99 3 18.75 3H5.25C4.01 3 3 4.01 3 5.25v13.5C3 19.99 4.01 21 5.25 21m-.531-3.219a.753.753 0 0 0 1.062 0l4.499-4.498a.75.75 0 0 0 0-1.061l-4.5-4.5a.75.75 0 0 0-1.061 1.06l3.969 3.97l-3.969 3.969a.75.75 0 0 0 0 1.06M22.5 19.5c0 .576-.11 1.125-.308 1.632l1.589 1.588a.75.75 0 0 1-1.062 1.061l-1.328-1.328a4.492 4.492 0 0 1-6.782 0l-1.328 1.328a.753.753 0 0 1-1.062 0a.75.75 0 0 1 0-1.06l1.588-1.589A4.5 4.5 0 0 1 13.5 19.5h-.75a.75.75 0 0 1 0-1.5h.75v-1.94l-1.281-1.28a.75.75 0 0 1 1.06-1.061L14.56 15H15c0-1.655 1.346-3 3-3s3 1.345 3 3h.44l1.28-1.281a.75.75 0 0 1 1.061 1.06L22.5 16.062V18h.75a.75.75 0 0 1 0 1.5zm-6-4.5h3a1.5 1.5 0 0 0-3 0m4.5 1.5h-6v3c0 1.654 1.346 3 3 3s3-1.346 3-3z"
      />
    </IconFrame>
  )
}
export const DebugConsole = DebugConsoleIcon

export function FolderIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M2.75 8.623v7.379a4 4 0 0 0 4 4h10.5a4 4 0 0 0 4-4v-5.69a4 4 0 0 0-4-4H12M2.75 8.624V6.998a3 3 0 0 1 3-3h2.9a2.5 2.5 0 0 1 1.768.732L12 6.313m-9.25 2.31h5.904a2.5 2.5 0 0 0 1.768-.732L12 6.313"
        fill="none"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.5"
      />
    </IconFrame>
  )
}
export const Folder = FolderIcon

export function FolderOpenIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="m3.882 18.043l4.041-5.623a4 4 0 0 1 3.249-1.665h8.752M3.882 18.043a3.65 3.65 0 0 0 2.777 1.277h8.343a4 4 0 0 0 3.405-1.9l2.918-4.734a1.287 1.287 0 0 0-1.115-1.931h-.286M3.882 18.043A3.65 3.65 0 0 1 3 15.661V7.424A2.744 2.744 0 0 1 5.744 4.68h2.653c.607 0 1.189.24 1.618.67l.911.91a1.83 1.83 0 0 0 1.294.537l4.044-.001a3.66 3.66 0 0 1 3.66 3.66v.299"
        fill="none"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.5"
      />
    </IconFrame>
  )
}
export const FolderOpen = FolderOpenIcon

export function FolderAddIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="none">
        <path
          clipRule="evenodd"
          d="M17.5 23a5.5 5.5 0 1 0 0-11a5.5 5.5 0 0 0 0 11m0-8.993a.5.5 0 0 1 .5.5V17h2.493a.5.5 0 1 1 0 1H18v2.494a.5.5 0 0 1-1 0V18h-2.493a.5.5 0 1 1 0-1H17v-2.493a.5.5 0 0 1 .5-.5"
          fill="currentColor"
          fillRule="evenodd"
        />
        <path
          d="M2.75 8.623v7.379a4 4 0 0 0 4 4h3.35M2.75 8.623V6.998a3 3 0 0 1 3-3h2.9a2.5 2.5 0 0 1 1.768.732L12 6.313m-9.25 2.31h5.904a2.5 2.5 0 0 0 1.768-.732L12 6.313m0 0l5.25-.002a4 4 0 0 1 4 4v.669"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
        />
      </g>
    </IconFrame>
  )
}
export const FolderAdd = FolderAddIcon

export function CubeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M6.049 7.983a.75.75 0 0 1 .967-.435L12 9.438l4.984-1.89a.75.75 0 1 1 .532 1.402l-4.766 1.81v5.49a.75.75 0 1 1-1.5 0v-5.49L6.484 8.95a.75.75 0 0 1-.435-.967m4.542-5.472a3.75 3.75 0 0 1 2.818 0l7.498 3.04A1.75 1.75 0 0 1 22 7.173v9.653a1.75 1.75 0 0 1-1.093 1.621l-7.498 3.04a3.75 3.75 0 0 1-2.818 0l-7.498-3.04A1.75 1.75 0 0 1 2 16.826V7.173A1.75 1.75 0 0 1 3.093 5.55zm2.254 1.39a2.25 2.25 0 0 0-1.69 0l-7.499 3.04a.25.25 0 0 0-.156.232v9.653a.25.25 0 0 0 .156.231l7.499 3.04a2.25 2.25 0 0 0 1.69 0l7.499-3.04a.25.25 0 0 0 .156-.231V7.173a.25.25 0 0 0-.156-.232z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Cube = CubeIcon
export const ToolCubeIcon = CubeIcon
export const ToolCube = CubeIcon

export function CubeFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M13.409 2.511a3.75 3.75 0 0 0-2.818 0l-7.498 3.04A1.75 1.75 0 0 0 2 7.173v9.653a1.75 1.75 0 0 0 1.093 1.621l7.498 3.04a3.75 3.75 0 0 0 2.818 0l7.498-3.04A1.75 1.75 0 0 0 22 16.826V7.173a1.75 1.75 0 0 0-1.093-1.622zm-7.36 5.472a.75.75 0 0 1 .967-.435L12 9.438l4.984-1.89a.75.75 0 1 1 .532 1.402l-4.766 1.81v5.49a.75.75 0 1 1-1.5 0v-5.49L6.484 8.95a.75.75 0 0 1-.435-.967"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const CubeFilled = CubeFilledIcon
export const ToolCubeFilledIcon = CubeFilledIcon
export const ToolCubeFilled = CubeFilledIcon

export function DocumentCubeFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M12 2v6a2 2 0 0 0 2 2h6v10a2 2 0 0 1-2 2h-7.404l.022-.011A2.5 2.5 0 0 0 12 19.753V19h4.25a.75.75 0 0 0 0-1.5H12V16h4.25a.75.75 0 0 0 0-1.5h-4.365a2.5 2.5 0 0 0-1.267-1.486l-.103-.051a.8.8 0 0 0 .235.037h5.5a.75.75 0 0 0 0-1.5h-5.5a.75.75 0 0 0-.428 1.366l-3.204-1.602a2.5 2.5 0 0 0-2.236 0l-.882.44V4a2 2 0 0 1 2-2zm1.5.5V8a.5.5 0 0 0 .5.5h5.5zm-3.33 11.408l-3.5-1.75a1.5 1.5 0 0 0-1.34 0l-3.5 1.75A1.5 1.5 0 0 0 1 15.25v4.503a1.5 1.5 0 0 0 .83 1.342l3.5 1.75a1.5 1.5 0 0 0 1.34 0l3.5-1.75a1.5 1.5 0 0 0 .83-1.342V15.25a1.5 1.5 0 0 0-.83-1.342m-7.617 1.368a.5.5 0 0 1 .67-.223L6 16.44l2.776-1.388a.5.5 0 1 1 .448.894L6.5 17.31v3.19a.5.5 0 1 1-1 0v-3.19l-2.724-1.363a.5.5 0 0 1-.223-.67"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const DocumentCubeFilled = DocumentCubeFilledIcon
export const ToolDocumentationFilledIcon = DocumentCubeFilledIcon
export const ToolDocumentationFilled = DocumentCubeFilledIcon

export function DocumentCubeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M10 12.25a.75.75 0 0 1 .75-.75h5.5a.75.75 0 0 1 0 1.5h-5.5a.763.763 0 0 1-.75-.75M12 16v-.75c0-.258-.04-.511-.115-.75h4.365a.75.75 0 0 1 0 1.5zm0 3v-1.5h4.25a.75.75 0 0 1 0 1.5zm-6.5-7.95q-.32.066-.618.214l-.882.44V4a2 2 0 0 1 2-2h6.172c.515 0 1.047.22 1.413.586l5.829 5.828A2 2 0 0 1 20 9.828V20a2 2 0 0 1-2 2h-7.404l.022-.011a2.5 2.5 0 0 0 1.268-1.489H18a.5.5 0 0 0 .5-.5V10H14a2 2 0 0 1-2-2V3.5H6a.5.5 0 0 0-.5.5zM17.378 8.5L13.5 4.621V8a.5.5 0 0 0 .5.5zm-7.207 5.408l-3.5-1.75a1.5 1.5 0 0 0-1.342 0l-3.5 1.75A1.5 1.5 0 0 0 1 15.25v4.503a1.5 1.5 0 0 0 .83 1.342l3.5 1.75a1.5 1.5 0 0 0 1.34 0l3.5-1.75a1.5 1.5 0 0 0 .83-1.342V15.25a1.5 1.5 0 0 0-.83-1.342m-7.618 1.368a.5.5 0 0 1 .67-.223L6 16.44l2.776-1.388a.5.5 0 1 1 .448.894L6.5 17.31v3.19a.5.5 0 1 1-1 0v-3.19l-2.724-1.363a.5.5 0 0 1-.223-.67"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const DocumentCube = DocumentCubeIcon
export const ToolDocumentationIcon = DocumentCubeIcon
export const ToolDocumentation = DocumentCubeIcon

export function ReceiptCubeFilledIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M4 5.25A2.25 2.25 0 0 1 6.25 3h9.5A2.25 2.25 0 0 1 18 5.25V14h4v3.75A3.25 3.25 0 0 1 18.75 21h-7.083c.214-.372.333-.8.333-1.247V15.25a2.5 2.5 0 0 0-1.382-2.236L9.59 12.5h4.16a.75.75 0 0 0 0-1.5h-5.5a.75.75 0 0 0-.7.48l-.432-.216a2.5 2.5 0 0 0-2.236 0l-.882.44zM18 19.5h.75a1.75 1.75 0 0 0 1.75-1.75V15.5H18zM7.5 7.75c0 .414.336.75.75.75h5.5a.75.75 0 0 0 0-1.5h-5.5a.75.75 0 0 0-.75.75m2.67 6.158l-3.5-1.75a1.5 1.5 0 0 0-1.34 0l-3.5 1.75A1.5 1.5 0 0 0 1 15.25v4.503a1.5 1.5 0 0 0 .83 1.342l3.5 1.75a1.5 1.5 0 0 0 1.34 0l3.5-1.75a1.5 1.5 0 0 0 .83-1.342V15.25a1.5 1.5 0 0 0-.83-1.342m-7.617 1.368a.5.5 0 0 1 .67-.223L6 16.44l2.776-1.388a.5.5 0 1 1 .448.894L6.5 17.31v3.19a.5.5 0 1 1-1 0v-3.19l-2.724-1.363a.5.5 0 0 1-.223-.67"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const ReceiptCubeFilled = ReceiptCubeFilledIcon
export const ToolReceiptFilledIcon = ReceiptCubeFilledIcon
export const ToolReceiptFilled = ReceiptCubeFilledIcon

export function ReceiptCubeIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M4 5.25A2.25 2.25 0 0 1 6.25 3h9.5A2.25 2.25 0 0 1 18 5.25V14h4v3.75A3.25 3.25 0 0 1 18.75 21h-7.083c.214-.372.333-.8.333-1.247V19.5h4.5V5.25a.75.75 0 0 0-.75-.75h-9.5a.75.75 0 0 0-.75.75v5.8q-.32.066-.618.214l-.882.44zm9.75 7.25H9.59l-2.04-1.02a.75.75 0 0 1 .7-.48h5.5a.75.75 0 0 1 0 1.5m4.25 7h.75a1.75 1.75 0 0 1 1.75-1.75V15.5H18zM8.25 7a.75.75 0 0 1 0 1.5h5.5a.75.75 0 0 0 0-1.5zm1.92 6.908l-3.5-1.75a1.5 1.5 0 0 0-1.34 0l-3.5 1.75A1.5 1.5 0 0 0 1 15.25v4.503a1.5 1.5 0 0 0 .83 1.342l3.5 1.75a1.5 1.5 0 0 0 1.34 0l3.5-1.75a1.5 1.5 0 0 0 .83-1.342V15.25a1.5 1.5 0 0 0-.83-1.342m-7.617 1.368a.5.5 0 0 1 .67-.223L6 16.44l2.776-1.388a.5.5 0 1 1 .448.894L6.5 17.31v3.19a.5.5 0 1 1-1 0v-3.19l-2.724-1.363a.5.5 0 0 1-.223-.67"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const ReceiptCube = ReceiptCubeIcon
export const ToolReceiptIcon = ReceiptCubeIcon
export const ToolReceipt = ReceiptCubeIcon

export function PanelSearchIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2">
        <rect width="7" height="18" x="3" y="3" rx="1" />
        <rect width="7" height="7" x="14" y="3" rx="1" />
        <circle cx="16.75" cy="16.75" r="2.25" />
        <path d="m18.4 18.4 2.1 2.1" />
      </g>
    </IconFrame>
  )
}

export const PanelSearch = PanelSearchIcon

export function ZoomIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 512 512" {...props}>
      <path d="M0 0h512v512H0z" fill="none" />
      <path
        d="m479.6 399.716l-81.084-81.084l-62.368-25.767A175 175 0 0 0 368 192c0-97.047-78.953-176-176-176S16 94.953 16 192s78.953 176 176 176a175.03 175.03 0 0 0 101.619-32.377l25.7 62.2l81.081 81.088a56 56 0 1 0 79.2-79.195M48 192c0-79.4 64.6-144 144-144s144 64.6 144 144s-64.6 144-144 144S48 271.4 48 192m408.971 264.284a24.03 24.03 0 0 1-33.942 0l-76.572-76.572l-23.894-57.835l57.837 23.894l76.573 76.572a24.03 24.03 0 0 1-.002 33.941"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Zoom = ZoomIcon
export const SearchZoomIcon = ZoomIcon
export const SearchZoom = ZoomIcon

export function WorkflowIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M9 10c.55 0 1-.45 1-1V7h4.14c.45 1.72 2 3 3.86 3c2.21 0 4-1.79 4-4s-1.79-4-4-4c-1.86 0-3.41 1.28-3.86 3H10V3c0-.55-.45-1-1-1H3c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h2v4.09L1.79 17.3a.996.996 0 0 0 0 1.41l3.5 3.5c.2.2.45.29.71.29s.51-.1.71-.29L9.92 19h4.09v2c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1h-6c-.55 0-1 .45-1 1v2H9.92l-2.91-2.91V10h2Zm9-6c1.1 0 2 .9 2 2s-.9 2-2 2s-2-.9-2-2s.9-2 2-2m-2 12h4v4h-4zM6 20.09L3.91 18L6 15.91L8.09 18zM4 4h4v4H4z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Workflow = WorkflowIcon

export function WorkflowFolderIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="none">
        <path
          d="M2.75 8.623v7.379a4 4 0 0 0 4 4h3.35M2.75 8.623V6.998a3 3 0 0 1 3-3h2.9a2.5 2.5 0 0 1 1.768.732L12 6.313m-9.25 2.31h5.904a2.5 2.5 0 0 0 1.768-.732L12 6.313m0 0l5.25-.002a4 4 0 0 1 4 4v.669"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
        />
        <g fill="currentColor" transform="translate(11.5 11.5) scale(0.5)">
          <path d="M9 10c.55 0 1-.45 1-1V7h4.14c.45 1.72 2 3 3.86 3c2.21 0 4-1.79 4-4s-1.79-4-4-4c-1.86 0-3.41 1.28-3.86 3H10V3c0-.55-.45-1-1-1H3c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h2v4.09L1.79 17.3a.996.996 0 0 0 0 1.41l3.5 3.5c.2.2.45.29.71.29s.51-.1.71-.29L9.92 19h4.09v2c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1h-6c-.55 0-1 .45-1 1v2H9.92l-2.91-2.91V10h2Zm9-6c1.1 0 2 .9 2 2s-.9 2-2 2s-2-.9-2-2s.9-2 2-2m-2 12h4v4h-4zM6 20.09L3.91 18L6 15.91L8.09 18zM4 4h4v4H4z" />
        </g>
      </g>
    </IconFrame>
  )
}
export const WorkflowFolder = WorkflowFolderIcon

export function FolderToolsIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="none">
        <path
          d="M2.75 8.623v7.379a4 4 0 0 0 4 4h3.35M2.75 8.623V6.998a3 3 0 0 1 3-3h2.9a2.5 2.5 0 0 1 1.768.732L12 6.313m-9.25 2.31h5.904a2.5 2.5 0 0 0 1.768-.732L12 6.313m0 0l5.25-.002a4 4 0 0 1 4 4v.669"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
        />
        <g fill="currentColor" transform="translate(11.5 11.5) scale(0.5)">
          <path d="M6.049 7.983a.75.75 0 0 1 .967-.435L12 9.438l4.984-1.89a.75.75 0 1 1 .532 1.402l-4.766 1.81v5.49a.75.75 0 1 1-1.5 0v-5.49L6.484 8.95a.75.75 0 0 1-.435-.967m4.542-5.472a3.75 3.75 0 0 1 2.818 0l7.498 3.04A1.75 1.75 0 0 1 22 7.173v9.653a1.75 1.75 0 0 1-1.093 1.621l-7.498 3.04a3.75 3.75 0 0 1-2.818 0l-7.498-3.04A1.75 1.75 0 0 1 2 16.826V7.173A1.75 1.75 0 0 1 3.093 5.55zm2.254 1.39a2.25 2.25 0 0 0-1.69 0l-7.499 3.04a.25.25 0 0 0-.156.232v9.653a.25.25 0 0 0 .156.231l7.499 3.04a2.25 2.25 0 0 0 1.69 0l7.499-3.04a.25.25 0 0 0 .156-.231V7.173a.25.25 0 0 0-.156-.232z" />
        </g>
      </g>
    </IconFrame>
  )
}
export const FolderTools = FolderToolsIcon
export const ToolFolderIcon = FolderToolsIcon
export const ToolFolder = FolderToolsIcon
export const FolderToolIcon = FolderToolsIcon
export const FolderTool = FolderToolsIcon


export function CollapseAllIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M7.4 22L6 20.6l6-6l6 6l-1.4 1.4l-4.6-4.6zM12 9.4l-6-6L7.4 2L12 6.6L16.6 2L18 3.4z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const CollapseAll = CollapseAllIcon

export function ExpandAllIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="m12 22l-6-6l1.425-1.425L12 19.15l4.575-4.575L18 16zM7.45 9.4L6 8l6-6l6 6l-1.45 1.4L12 4.85z"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export function ChevronRightIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M9.5 6L8.1 7.4L12.7 12L8.1 16.6L9.5 18L15.5 12L9.5 6Z" fill="currentColor" />
    </IconFrame>
  )
}
export const ChevronRight = ChevronRightIcon

export function SortIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z" fill="currentColor" />
    </IconFrame>
  )
}
export const Sort = SortIcon

export function NodeEditIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="none" stroke="currentColor" strokeLinejoin="round" strokeWidth="1.5">
        <path d="m20.689 3.934l-.623-.623a1.063 1.063 0 0 0-1.503 0l-3.349 3.35a3.2 3.2 0 0 0-.872 1.628L14 10l1.71-.342a3.2 3.2 0 0 0 1.63-.872l3.349-3.349a1.063 1.063 0 0 0 0-1.503Z" />
        <path
          strokeLinecap="square"
          d="M13.69 19.457c-.19-.46-.19-1.042-.19-2.207s0-1.747.19-2.207a2.5 2.5 0 0 1 1.353-1.353c.46-.19 1.042-.19 2.207-.19s1.747 0 2.207.19a2.5 2.5 0 0 1 1.353 1.353c.19.46.19 1.042.19 2.207s0 1.747-.19 2.207a2.5 2.5 0 0 1-1.353 1.353c-.46.19-1.042.19-2.207.19s-1.747 0-2.207-.19a2.5 2.5 0 0 1-1.353-1.353Zm-10.5 0C3 18.997 3 18.415 3 17.25s0-1.747.19-2.207a2.5 2.5 0 0 1 1.353-1.353c.46-.19 1.042-.19 2.207-.19s1.747 0 2.207.19a2.5 2.5 0 0 1 1.353 1.353c.19.46.19 1.042.19 2.207s0 1.747-.19 2.207a2.5 2.5 0 0 1-1.353 1.353c-.46.19-1.042.19-2.207.19s-1.747 0-2.207-.19a2.5 2.5 0 0 1-1.353-1.353Zm0-10.5C3 8.497 3 7.915 3 6.75s0-1.747.19-2.207A2.5 2.5 0 0 1 4.543 3.19C5.003 3 5.585 3 6.75 3s1.747 0 2.207.19a2.5 2.5 0 0 1 1.353 1.353c.19.46.19 1.042.19 2.207s0 1.747-.19 2.207a2.5 2.5 0 0 1-1.353 1.353c-.46.19-1.042.19-2.207.19s-1.747 0-2.207-.19A2.5 2.5 0 0 1 3.19 8.957Z"
        />
      </g>
    </IconFrame>
  )
}
export const NodeEdit = NodeEditIcon

export function ArrowRightIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 12 24" {...props}>
      <path d="M0 0h12v24H0z" fill="none" />
      <path
        d="M10.157 12.711L4.5 18.368l-1.414-1.414l4.95-4.95l-4.95-4.95L4.5 5.64l5.657 5.657a1 1 0 0 1 0 1.414"
        fill="currentColor"
        fillRule="evenodd"
      />
    </IconFrame>
  )
}
export const ArrowRight = ArrowRightIcon

export function PathIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="m19.85 8.14l-.46-2.32c-.33-1.63-1.77-2.81-3.43-2.81s-3 1.09-3.39 2.65L9.52 17.87c-.17.67-.76 1.14-1.45 1.14S6.74 18.5 6.6 17.8l-.41-2.03a3.01 3.01 0 0 0 1.83-2.76c0-1.65-1.35-3-3-3s-3 1.35-3 3c0 1.36.91 2.5 2.15 2.86l.46 2.32C4.96 19.82 6.4 21 8.06 21s3-1.09 3.39-2.65L14.5 6.14c.17-.67.76-1.14 1.45-1.14s1.33.51 1.47 1.21l.41 2.03A3.01 3.01 0 0 0 16 11c0 1.65 1.35 3 3 3s3-1.35 3-3c0-1.36-.91-2.5-2.15-2.86M5 12c.55 0 1 .45 1 1s-.45 1-1 1s-1-.45-1-1s.45-1 1-1m14 0c-.55 0-1-.45-1-1s.45-1 1-1s1 .45 1 1s-.45 1-1 1"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const Path = PathIcon

export function MapConnectionIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="none">
        <path d="M9 6a3 3 0 1 1-6 0a3 3 0 0 1 6 0" />
        <path
          d="M9 6h9a3 3 0 1 1 0 6H6a3 3 0 1 0 0 6h12M9 6a3 3 0 1 1-6 0a3 3 0 0 1 6 0Zm11 12l-2 1.5v-3z"
          stroke="currentColor"
          strokeLinecap="square"
          strokeWidth="2"
        />
      </g>
    </IconFrame>
  )
}
export const MapConnection = MapConnectionIcon

export function BookmarkStarIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path fill="currentColor" d="M18 2H6c-1.1 0-2 .9-2 2v17c0 .36.19.69.5.87s.69.18 1 0l6.5-3.72l6.5 3.72c.15.09.32.13.5.13s.35-.04.5-.13c.31-.18.5-.51.5-.87V4c0-1.1-.9-2-2-2m0 8v9.28l-5.5-3.14a.98.98 0 0 0-.99 0l-5.5 3.14V4h12v6Z" />
      <path fill="currentColor" d="M13.08 8.4L12 6l-1.08 2.4l-2.52.2l2 1.8l-.8 2.8l2.4-1.6l2.4 1.6l-.8-2.8l2-1.8z" />
    </IconFrame>
  )
}
export const BookmarkStar = BookmarkStarIcon

export function StarIcon({ isFavorite, ...props }: CodeSquareFilledIconProps & { isFavorite?: boolean }): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"
        fill={isFavorite ? '#eab308' : 'currentColor'}
      />
    </IconFrame>
  )
}
export const FavoriteIcon = StarIcon
export const Favorite = StarIcon
export const Star = StarIcon

export const DeleteIcon = TrashIcon
export const Delete = TrashIcon

export function FoldedScriptIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path fill="currentColor" d="M9.197 10a.75.75 0 0 0 0 1.5h6.5a.75.75 0 0 0 0-1.5zm-2.382 4a.75.75 0 0 0 0 1.5h6.5a.75.75 0 0 0 0-1.5zm-1.581 4a.75.75 0 0 0 0 1.5h6.5a.75.75 0 0 0 0-1.5z" />
      <path fill="currentColor" d="M4.125 0h15.75a4.1 4.1 0 0 1 2.92 1.205A4.1 4.1 0 0 1 24 4.125c0 1.384-.476 2.794-1.128 4.16c-.652 1.365-1.515 2.757-2.352 4.104l-.008.013c-.849 1.368-1.669 2.691-2.28 3.97c-.614 1.283-.982 2.45-.982 3.503a2.625 2.625 0 1 0 4.083-2.183a.75.75 0 1 1 .834-1.247A4.126 4.126 0 0 1 19.875 24H4.5a4.125 4.125 0 0 1-4.125-4.125c0-2.234 1.258-4.656 2.59-6.902c.348-.586.702-1.162 1.05-1.728c.8-1.304 1.567-2.553 2.144-3.738H3.39c-.823 0-1.886-.193-2.567-1.035A3.65 3.65 0 0 1 0 4.125A4.125 4.125 0 0 1 4.125 0M15.75 19.875c0-1.38.476-2.786 1.128-4.15c.649-1.358 1.509-2.743 2.343-4.086l.017-.028c.849-1.367 1.669-2.692 2.28-3.972c.614-1.285.982-2.457.982-3.514A2.615 2.615 0 0 0 19.875 1.5a2.625 2.625 0 0 0-2.625 2.625c0 .865.421 1.509 1.167 2.009A.75.75 0 0 1 18 7.507H7.812c-.65 1.483-1.624 3.069-2.577 4.619c-.334.544-.666 1.083-.98 1.612c-1.355 2.287-2.38 4.371-2.38 6.137A2.625 2.625 0 0 0 4.5 22.5h12.193a4.1 4.1 0 0 1-.943-2.625M1.5 4.125c-.01.511.163 1.008.487 1.403c.254.313.74.479 1.402.479h12.86a3.65 3.65 0 0 1-.499-1.882a4.1 4.1 0 0 1 .943-2.625H4.125A2.625 2.625 0 0 0 1.5 4.125" />
    </IconFrame>
  )
}
export const FoldedScript = FoldedScriptIcon
export const Log = FoldedScriptIcon
export const LogIcon = FoldedScriptIcon

export function SteeringWheelIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path
        d="M23 12.75v-1.5h-3.29a7.7 7.7 0 0 0-1.73-4.17l2.325-2.325l-1.06-1.06L16.92 6.02a7.7 7.7 0 0 0-4.17-1.73V1h-1.5v3.29a7.7 7.7 0 0 0-4.17 1.73L4.755 3.695l-1.06 1.06L6.02 7.08a7.7 7.7 0 0 0-1.73 4.17H1v1.5h3.29a7.7 7.7 0 0 0 1.73 4.17l-2.325 2.325l1.06 1.06L7.08 17.98a7.7 7.7 0 0 0 4.17 1.73V23h1.5v-3.29a7.7 7.7 0 0 0 4.17-1.73l2.325 2.325l1.06-1.06l-2.325-2.325a7.7 7.7 0 0 0 1.73-4.17zm-4.8-1.5h-2.525a3.6 3.6 0 0 0-.55-1.315L16.91 8.15c.69.875 1.15 1.94 1.29 3.1m-6.2 3c-1.24 0-2.25-1.01-2.25-2.25S10.76 9.75 12 9.75s2.25 1.01 2.25 2.25s-1.01 2.25-2.25 2.25m3.85-7.16l-1.785 1.785c-.395-.26-.84-.45-1.315-.55V5.8c1.16.14 2.225.6 3.1 1.29m-4.6-1.29v2.525c-.48.095-.92.285-1.315.55L8.15 7.09a6.2 6.2 0 0 1 3.1-1.29M7.09 8.15l1.785 1.785c-.26.395-.45.84-.55 1.315H5.8c.14-1.16.6-2.225 1.29-3.1m-1.29 4.6h2.525c.095.48.285.925.55 1.315L7.09 15.85a6.2 6.2 0 0 1-1.29-3.1m2.35 4.16l1.785-1.785c.395.26.84.45 1.315.55V18.2a6.2 6.2 0 0 1-3.1-1.29m4.6 1.29v-2.525c.48-.095.92-.285 1.315-.55l1.785 1.785a6.2 6.2 0 0 1-3.1 1.29m4.16-2.35l-1.785-1.785c.26-.395.45-.84.55-1.315H18.2a6.2 6.2 0 0 1-1.29 3.1"
        fill="currentColor"
      />
    </IconFrame>
  )
}
export const SteeringWheel = SteeringWheelIcon
export const RuntimeIcon = SteeringWheelIcon
export const Runtime = SteeringWheelIcon

export function SquareOutlineIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path fill="currentColor" d="M3 21V3h18v18zm2-2h14V5H5zm0 0V5z" />
    </IconFrame>
  )
}


export function DragGridVerticalIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path fill="currentColor" d="M9 3h2v2H9zm4 0h2v2h-2zM9 7h2v2H9zm4 0h2v2h-2zm-4 4h2v2H9zm4 0h2v2h-2zm-4 4h2v2H9zm4 0h2v2h-2zm-4 4h2v2H9zm4 0h2v2h-2z" />
    </IconFrame>
  )
}

export function DragGridHorizontalIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <path fill="currentColor" d="M3 9h2v2H3zm4 0h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2zm4 0h2v2h-2zM3 13h2v2H3zm4 0h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2zm4 0h2v2h-2z" />
    </IconFrame>
  )
}

export function HollowHexagonIcon(props: CodeSquareFilledIconProps): ReactElement {
  return (
    <IconFrame viewBox="0 0 24 24" {...props}>
      <path d="M0 0h24v24H0z" fill="none" />
      <g fill="none">
        <path clipRule="evenodd" d="M20.66 7L12 2L3.34 7v10L12 22l8.66-5zM12 16a4 4 0 1 0 0-8a4 4 0 0 0 0 8" />
        <path d="M16 12a4 4 0 1 1-8 0a4 4 0 0 1 8 0" />
        <path d="m12 2l8.66 5v10L12 22l-8.66-5V7z" stroke="currentColor" strokeLinecap="square" strokeWidth="2" />
        <path d="M16 12a4 4 0 1 1-8 0a4 4 0 0 1 8 0Z" stroke="currentColor" strokeLinecap="square" strokeWidth="2" />
      </g>
    </IconFrame>
  )
}

export const HexagonHollowIcon = HollowHexagonIcon
export const HollowHexagon = HollowHexagonIcon


/**
 * Renders a pre-resolved node presentation. Identity lookup belongs to the shared
 * Studio presentation index, not this visual component.
 */
export function WorkflowNodeIcon({ descriptor, size = 20, title, className, ...svgProps }: WorkflowNodeIconProps): ReactElement {
  const [failedHref, setFailedHref] = useState<string>()
  if (descriptor.source === 'image' && failedHref !== descriptor.href) {
    return <img alt={descriptor.alt ?? ''} className={className} height={size} onError={() => setFailedHref(descriptor.href)} src={descriptor.href} title={title} width={size} />
  }
  const icon = descriptor.source === 'image' ? descriptor.fallback : descriptor.name
  switch (icon) {
    case 'manual-trigger':
      return (
        <IconFrame {...svgProps} className={className} size={size} title={title}>
          <path d="M7 4.5 18.5 12 7 19.5V4.5Z" fill="currentColor" />
          <path d="M4.5 7.25v9.5" stroke="currentColor" strokeLinecap="square" strokeWidth="1.5" />
        </IconFrame>
      )
    case 'meshingress-api':
      return (
        <IconFrame {...svgProps} className={className} size={size} title={title}>
          <path d="M18.437 11H5.565a2.5 2.5 0 0 1-2.5-2.5V5.564a2.5 2.5 0 0 1 2.5-2.5h12.872a2.5 2.5 0 0 1 2.5 2.5V8.5a2.5 2.5 0 0 1-2.5 2.5M5.565 4.064a1.5 1.5 0 0 0-1.5 1.5V8.5a1.5 1.5 0 0 0 1.5 1.5h12.872a1.5 1.5 0 0 0 1.5-1.5V5.564a1.5 1.5 0 0 0-1.5-1.5Zm12.872 16.872H5.565a2.5 2.5 0 0 1-2.5-2.5V15.5a2.5 2.5 0 0 1 2.5-2.5h12.872a2.5 2.5 0 0 1 2.5 2.5v2.934a2.5 2.5 0 0 1-2.5 2.502M5.565 14a1.5 1.5 0 0 0-1.5 1.5v2.934a1.5 1.5 0 0 0 1.5 1.5h12.872a1.5 1.5 0 0 0 1.5-1.5V15.5a1.5 1.5 0 0 0-1.5-1.5Z" fill="currentColor" />
        </IconFrame>
      )
    case 'folder':
      return <FolderIcon {...svgProps} className={className} size={size} title={title} />
    case 'hollow-hexagon':
      return <HollowHexagonIcon {...svgProps} className={className} size={size} title={title} />
    default:
      return (
        <CubeIcon {...svgProps} className={className} size={size} title={title} />
      )
  }
}
