import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProfilePage from './pages/ProfilePage'
import BookPage from './pages/BookPage'
import BookingConfirmationPage from './pages/BookingConfirmationPage'
import AppointmentsPage from './pages/AppointmentsPage'
import AppointmentDetailPage from './pages/AppointmentDetailPage'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/book"
            element={
              <ProtectedRoute>
                <BookPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/booking-confirmation"
            element={
              <ProtectedRoute>
                <BookingConfirmationPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/appointments"
            element={
              <ProtectedRoute>
                <AppointmentsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/appointments/:id"
            element={
              <ProtectedRoute>
                <AppointmentDetailPage />
              </ProtectedRoute>
            }
          />
          <Route path="/admin/*" element={<div className="p-8">Admin — Phase 10</div>} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
