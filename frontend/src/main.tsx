import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './SwissApp'
import './swiss.css'

document.documentElement.dataset.theme = 'light'

createRoot(document.getElementById('app')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
