import { BrowserRouter, Route, Routes } from 'react-router-dom'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<div className="p-8 text-2xl font-bold">CapiBook — Coming Soon</div>} />
        <Route path="/login" element={<div className="p-8">Login — Phase 1</div>} />
        <Route path="/register" element={<div className="p-8">Register — Phase 1</div>} />
        <Route path="/profile" element={<div className="p-8">Profile — Phase 1</div>} />
        <Route path="/book" element={<div className="p-8">Book Appointment — Phase 4</div>} />
        <Route path="/appointments" element={<div className="p-8">My Appointments — Phase 4</div>} />
        <Route path="/appointments/:id" element={<div className="p-8">Appointment Detail — Phase 4</div>} />
        <Route path="/admin" element={<div className="p-8">Admin Dashboard — Phase 10</div>} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
