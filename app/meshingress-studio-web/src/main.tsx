import './utils/uuid'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { RuntimeConfiguration } from './runtime/RuntimeConfiguration'
// @ts-ignore
import App from './App.tsx'

async function bootstrap() {
  await RuntimeConfiguration.load()
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )

  requestAnimationFrame(() => {
    void RuntimeConfiguration.loadGoogleClientIdInBackground()
  })
}

void bootstrap()
