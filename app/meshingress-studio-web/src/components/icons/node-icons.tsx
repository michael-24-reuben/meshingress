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
  ...svgProps
}: IconFrameProps): ReactElement {
  const isDecorative = ariaHidden ?? !(ariaLabel || title)

  return (
    <svg
      {...svgProps}
      aria-hidden={isDecorative || undefined}
      aria-label={isDecorative ? undefined : ariaLabel}
      className={className}
      fill="none"
      height={size}
      role={isDecorative ? undefined : 'img'}
      viewBox="0 0 24 24"
      width={size}
      xmlns="http://www.w3.org/2000/svg"
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
