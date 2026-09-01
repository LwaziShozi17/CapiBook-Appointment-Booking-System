import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import AdminRoute from './components/AdminRoute'
import AdminLayout from './components/AdminLayout'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProfilePage from './pages/ProfilePage'
import BookPage from './pages/BookPage'
import BookingConfirmationPage from './pages/BookingConfirmationPage'
import AppointmentsPage from './pages/AppointmentsPage'
import AppointmentDetailPage from './pages/AppointmentDetailPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminAppointmentsPage from './pages/admin/AdminAppointmentsPage'
import AdminBranchesPage from './pages/admin/AdminBranchesPage'
import AdminServicesPage from './pages/admin/AdminServicesPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'
import AdminAnalyticsPage from './pages/admin/AdminAnalyticsPage'
import AdminAuditPage from './pages/admin/AdminAuditPage'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Customer-facing routes with Navbar */}
          <Route
            path="/*"
            element={
              <>
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
                    path="/appointments/new"
                    element={
                      <ProtectedRoute>
                        <Navigate to="/book" replace />
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
                </Routes>
              </>
            }
          />

          {/* Admin routes — sidebar layout, no customer Navbar */}
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminLayout />
              </AdminRoute>
            }
          >
            <Route index element={<AdminDashboardPage />} />
            <Route path="appointments" element={<AdminAppointmentsPage />} />
            <Route
              path="branches"
              element={
                <AdminRoute requireRole="SYSTEM_ADMIN">
                  <AdminBranchesPage />
                </AdminRoute>
              }
            />
            <Route
              path="services"
              element={
                <AdminRoute requireRole="SYSTEM_ADMIN">
                  <AdminServicesPage />
                </AdminRoute>
              }
            />
            <Route
              path="users"
              element={
                <AdminRoute requireRole="SYSTEM_ADMIN">
                  <AdminUsersPage />
                </AdminRoute>
              }
            />
            <Route
              path="analytics"
              element={
                <AdminRoute requireRole="SYSTEM_ADMIN">
                  <AdminAnalyticsPage />
                </AdminRoute>
              }
            />
            <Route
              path="audit"
              element={
                <AdminRoute requireRole="SYSTEM_ADMIN">
                  <AdminAuditPage />
                </AdminRoute>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
