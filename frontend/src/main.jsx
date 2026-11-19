import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'sonner';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { CompareProvider } from './context/CompareContext';
import { LanguageProvider } from './context/LanguageContext';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <LanguageProvider>
        <AuthProvider>
          <CartProvider>
            <CompareProvider>
              <App />
            </CompareProvider>
            <Toaster
              position="top-right"
              className="app-toaster"
              closeButton
              duration={3000}
              toastOptions={{
                style: {
                  fontFamily: 'Inter, system-ui, sans-serif',
                },
              }}
            />
          </CartProvider>
        </AuthProvider>
      </LanguageProvider>
    </BrowserRouter>
  </StrictMode>
);
