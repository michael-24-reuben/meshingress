import type { ReactElement } from "react"
import { IconFrame, type IconFrameProps } from "./node-icons"

export function AvailabilityDisable(props: IconFrameProps): ReactElement {
    return (
        <IconFrame viewBox="0 0 24 24" {...props}>
            <path d="M0 0h24v24H0z" fill="none" />
            <path fill="none" stroke="currentColor" strokeLinejoin="round" d="m3.5 3.5l9 9m2-4.5a6.5 6.5 0 1 1-13 0a6.5 6.5 0 0 1 13 0Z" />
        </IconFrame>
    )
}

export function AvailabilityUnknown(props: IconFrameProps): ReactElement {
    return (
        <IconFrame viewBox="0 0 24 24" {...props}>
            <path d="M0 0h32v32H0z" fill="none" />
            <circle cx="16" cy="22.5" r="1.5" fill="currentColor" />
            <path fill="currentColor" d="M17 19h-2v-4h2c1.103 0 2-.897 2-2s-.897-2-2-2h-2c-1.103 0-2 .897-2 2v.5h-2V13c0-2.206 1.794-4 4-4h2c2.206 0 4 1.794 4 4s-1.794 4-4 4z" />
            <path fill="currentColor" d="M29.391 14.527L17.473 2.609A2.08 2.08 0 0 0 16 2c-.533 0-1.067.203-1.473.609L2.609 14.527C2.203 14.933 2 15.466 2 16s.203 1.067.609 1.473L14.526 29.39c.407.407.941.61 1.474.61s1.067-.203 1.473-.609L29.39 17.474c.407-.407.61-.94.61-1.474s-.203-1.067-.609-1.473M16 28.036L3.965 16L16 3.964L28.036 16z" />
        </IconFrame>
    );
}

export function AvailabilityTimeConstricted(props: IconFrameProps): ReactElement {
    return (
        <IconFrame viewBox="0 0 24 24" {...props}>
            <path d="M0 0h24v24H0z" fill="none" />
            <path fill="currentColor" d="M14.221 2.247a1 1 0 0 0-.442 1.95L14 3.224zM4.198 13.778a1 1 0 0 0-1.95.443L3.223 14zM13 7a1 1 0 1 0-2 0zm-1 5h-1a1 1 0 0 0 .293.707zm2.293 3.707a1 1 0 1 0 1.414-1.415L15 15zM2.248 9.778a1 1 0 1 0 1.95.443L3.223 10zm2.106-.14a1 1 0 1 0-1.91-.59l.955.295zM4.668 5.2a1 1 0 1 0 1.466 1.36L5.4 5.88zm1.893.933A1 1 0 0 0 5.2 4.667l.68.733zm2.488-3.69a1 1 0 1 0 .59 1.91l-.296-.955zm1.172 1.755a1 1 0 0 0-.442-1.95l.221.975zM21.001 12h-1a8 8 0 0 1-8 8v2c5.522 0 10-4.477 10-10zm-7-8.777l-.222.975A8 8 0 0 1 20 12h2c0-4.76-3.325-8.742-7.779-9.753zM12 21v-1a8 8 0 0 1-7.803-6.222L3.223 14l-.975.22C3.258 18.675 7.24 22 12 22zm0-14h-1v5h2V7zm0 5l-.708.707l3 3L15 15l.707-.707l-3-3zm-8.778-2l.975.22q.068-.294.156-.582L3.4 9.343l-.956-.295q-.111.36-.195.73zM5.4 5.88l.733.68q.206-.221.427-.427L5.88 5.4l-.68-.733q-.277.257-.533.533zm3.942-2.482l.295.956q.288-.09.583-.156L10 3.223l-.222-.976a10 10 0 0 0-.73.196z" />
        </IconFrame>
    );
}



export function AvailabilityIcon({ availability, ...props }: IconFrameProps & { availability: string }): ReactElement {
    switch (availability) {
        case 'disable':
            return <AvailabilityDisable {...props} />
        case 'unknown':
            return <AvailabilityUnknown {...props} />
        case 'timeConstricted':
            return <AvailabilityTimeConstricted {...props} />
        default:
            return <AvailabilityUnknown {...props} />
    }
}
