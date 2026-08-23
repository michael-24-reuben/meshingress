import type { LogEntry } from '../../types'
import { Empty } from '../elements/Empty'

export interface CurrentExecutionPanelProps {
  logs: LogEntry[]
}

export function CurrentExecutionPanel({ logs }: CurrentExecutionPanelProps) {
  if (!logs.length) {
    return <Empty message="No current execution. Run the workflow to populate live logs." />
  }

  return (
    <>
      {logs.map((log) => (
        <div className={`log-row${log.severity ? ` ${log.severity}` : ''}`} key={log.id ?? `${log.time}-${log.source}`}>
          <span className="log-time">{log.time}</span>
          <strong>{log.source}</strong>
          <span>{log.message}</span>
        </div>
      ))}
    </>
  )
}

export default CurrentExecutionPanel
