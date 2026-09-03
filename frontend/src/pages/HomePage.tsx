import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function HomePage() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="flex flex-col flex-1">
      {/* Hero */}
      <section className="bg-white px-6 py-20 lg:py-28 border-b border-[#e2e8f0]">
        <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-center">
          <div>
            <h1 className="text-5xl lg:text-6xl font-extrabold text-[#0f172a] leading-[1.05] tracking-tight mb-6">
              Book your Capitec appointment online
            </h1>
            <p className="text-lg text-[#64748b] mb-10 max-w-md leading-relaxed">
              Choose a service, find a branch near you, and pick a time that works. No queuing, no callbacks.
            </p>
            <div className="flex items-center gap-3 flex-wrap">
              <Link
                to={isAuthenticated ? '/book' : '/login'}
                className="inline-flex items-center gap-2 bg-[#009de0] hover:bg-[#0085c3] text-white font-semibold px-6 py-3.5 rounded-xl transition-colors text-sm"
              >
                Book an appointment
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </Link>
              {!isAuthenticated && (
                <Link
                  to="/register"
                  className="inline-flex items-center gap-2 text-[#0f172a] hover:text-[#009de0] font-semibold px-6 py-3.5 rounded-xl border border-[#e2e8f0] hover:border-[#009de0]/40 transition-colors text-sm bg-white"
                >
                  Create an account
                </Link>
              )}
            </div>
          </div>

          {/* Decorative booking confirmation card */}
          <div className="hidden lg:flex items-center justify-center">
            <div className="relative w-full max-w-sm">
              {/* Background blob */}
              <div className="absolute inset-0 bg-[#eff9ff] rounded-3xl -rotate-2" />
              <div className="absolute inset-0 bg-[#e0f2fe] rounded-3xl rotate-1 opacity-60" />

              {/* Confirmation card */}
              <div className="relative bg-white rounded-2xl shadow-xl p-6 border border-[#e2e8f0]">
                <div className="flex items-center gap-3 mb-5 pb-5 border-b border-[#f1f5f9]">
                  <div className="w-9 h-9 bg-[#f0fdf4] rounded-full flex items-center justify-center flex-shrink-0">
                    <svg className="w-5 h-5 text-[#16a34a]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-[#16a34a]">Booking confirmed</p>
                    <p className="text-xs text-[#94a3b8]">Confirmation sent to your email</p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 mb-5">
                  <div>
                    <p className="text-xs text-[#94a3b8] mb-0.5 font-medium">Service</p>
                    <p className="text-sm font-semibold text-[#0f172a]">Home Loan</p>
                  </div>
                  <div>
                    <p className="text-xs text-[#94a3b8] mb-0.5 font-medium">Branch</p>
                    <p className="text-sm font-semibold text-[#0f172a]">Sandton City</p>
                  </div>
                  <div>
                    <p className="text-xs text-[#94a3b8] mb-0.5 font-medium">Date</p>
                    <p className="text-sm font-semibold text-[#0f172a]">Thu, 12 Sep</p>
                  </div>
                  <div>
                    <p className="text-xs text-[#94a3b8] mb-0.5 font-medium">Time</p>
                    <p className="text-sm font-semibold text-[#0f172a]">10:00 AM</p>
                  </div>
                </div>

                <div className="bg-[#f8fafc] rounded-xl px-4 py-3 flex items-center justify-between">
                  <p className="text-xs text-[#94a3b8] font-medium">Reference</p>
                  <p className="font-mono text-sm font-bold text-[#009de0]">CAP-2025-A3F1E</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="py-20 px-6 bg-[#f8fafc]">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-3xl font-bold text-[#0f172a] mb-3">How it works</h2>
          <p className="text-[#64748b] mb-14">Three steps to skip the queue.</p>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
            <StepCard
              step="1"
              title="Choose your service"
              description="Pick from Capitec's range of banking services — home loans, accounts, cards, investments, and more."
              icon={
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
              }
            />
            <StepCard
              step="2"
              title="Find a branch"
              description="Browse branches across South Africa. Filter by city, province, or proximity."
              icon={
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              }
            />
            <StepCard
              step="3"
              title="Pick a time slot"
              description="See real-time availability and lock in a slot that fits your schedule. Reschedule anytime."
              icon={
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              }
            />
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 px-6 bg-white border-t border-[#e2e8f0]">
        <div className="max-w-lg mx-auto text-center">
          <h2 className="text-3xl font-bold text-[#0f172a] mb-4">
            Ready to skip the queue?
          </h2>
          <p className="text-[#64748b] mb-8">
            Create an account and book your first appointment in under two minutes.
          </p>
          <Link
            to={isAuthenticated ? '/book' : '/register'}
            className="inline-block bg-[#009de0] hover:bg-[#0085c3] text-white font-semibold px-8 py-3.5 rounded-xl transition-colors text-sm"
          >
            {isAuthenticated ? 'Book now' : 'Get started'}
          </Link>
          {!isAuthenticated && (
            <p className="mt-4 text-sm text-[#94a3b8]">
              Already have an account?{' '}
              <Link to="/login" className="text-[#009de0] hover:underline font-medium">
                Sign in
              </Link>
            </p>
          )}
        </div>
      </section>
    </div>
  )
}

function StepCard({
  step,
  title,
  description,
  icon,
}: {
  step: string
  title: string
  description: string
  icon: React.ReactNode
}) {
  return (
    <div className="relative">
      <span className="absolute -top-3 -left-1 text-8xl font-extrabold text-[#009de0]/8 leading-none select-none pointer-events-none">
        {step}
      </span>
      <div className="relative">
        <div className="w-10 h-10 bg-[#eff9ff] text-[#009de0] rounded-xl flex items-center justify-center mb-4">
          {icon}
        </div>
        <h3 className="text-base font-semibold text-[#0f172a] mb-2">{title}</h3>
        <p className="text-sm text-[#64748b] leading-relaxed">{description}</p>
      </div>
    </div>
  )
}
