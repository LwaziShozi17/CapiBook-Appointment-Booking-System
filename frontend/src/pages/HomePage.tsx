import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function HomePage() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="min-h-screen bg-gray-50">
      <section className="bg-white py-20 px-6 text-center border-b border-gray-100">
        <div className="max-w-2xl mx-auto">
          <div className="inline-flex items-center gap-2 bg-purple-50 text-purple-700 text-sm font-medium px-3 py-1 rounded-full mb-6">
            <span className="w-2 h-2 bg-purple-500 rounded-full" />
            Skip the queue — book online
          </div>
          <h1 className="text-5xl font-bold text-gray-900 mb-5 leading-tight">
            Book Your Capitec<br />Appointment Online
          </h1>
          <p className="text-lg text-gray-500 mb-8 max-w-xl mx-auto">
            Choose your service, pick a branch near you, and select a time that works for you.
            No waiting in line.
          </p>
          <Link
            to={isAuthenticated ? '/book' : '/login'}
            className="inline-block bg-purple-600 hover:bg-purple-700 text-white font-semibold px-8 py-3 rounded-xl transition-colors text-base"
          >
            Book an Appointment
          </Link>
          {!isAuthenticated && (
            <p className="mt-4 text-sm text-gray-400">
              New here?{' '}
              <Link to="/register" className="text-purple-600 hover:underline font-medium">
                Create a free account
              </Link>
            </p>
          )}
        </div>
      </section>

      <section className="py-16 px-6">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-2xl font-semibold text-gray-900 text-center mb-10">
            How it works
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <FeatureCard
              step="1"
              title="Choose Your Service"
              description="Select from a range of Capitec banking services — account queries, loans, cards, and more."
              icon={
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
              }
            />
            <FeatureCard
              step="2"
              title="Pick a Branch"
              description="Find the Capitec branch closest to you. Browse by city or province."
              icon={
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              }
            />
            <FeatureCard
              step="3"
              title="Select a Time"
              description="See real-time slot availability and pick a time that fits your schedule."
              icon={
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              }
            />
          </div>
        </div>
      </section>

      <section className="py-12 px-6 bg-purple-600 text-white text-center">
        <div className="max-w-xl mx-auto">
          <h2 className="text-2xl font-semibold mb-3">Ready to get started?</h2>
          <p className="text-purple-100 mb-6">Create your account in seconds and book your first appointment today.</p>
          <Link
            to={isAuthenticated ? '/book' : '/register'}
            className="inline-block bg-white text-purple-700 font-semibold px-8 py-3 rounded-xl hover:bg-purple-50 transition-colors"
          >
            {isAuthenticated ? 'Book Now' : 'Get Started'}
          </Link>
        </div>
      </section>
    </div>
  )
}

function FeatureCard({
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
    <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm text-left">
      <div className="flex items-center gap-3 mb-4">
        <div className="w-10 h-10 bg-purple-100 text-purple-600 rounded-xl flex items-center justify-center flex-shrink-0">
          {icon}
        </div>
        <span className="text-xs font-semibold text-purple-600 uppercase tracking-wide">Step {step}</span>
      </div>
      <h3 className="text-base font-semibold text-gray-900 mb-2">{title}</h3>
      <p className="text-sm text-gray-500 leading-relaxed">{description}</p>
    </div>
  )
}
