import { useState } from 'react'
import { getAppointmentSummary, getBranchUtilisation, getServicePopularity } from '../../api/admin'
import { getBranches } from '../../api/branches'
import type {
  AppointmentSummaryResponse,
  BranchUtilisationResponse,
  ServicePopularityResponse,
  BranchResponse,
} from '../../types'

function todayStr(): string {
  return new Date().toISOString().split('T')[0]
}

function thirtyDaysAgo(): string {
  const d = new Date()
  d.setDate(d.getDate() - 30)
  return d.toISOString().split('T')[0]
}

export default function AdminAnalyticsPage() {
  const [from, setFrom] = useState(thirtyDaysAgo())
  const [to, setTo] = useState(todayStr())

  const [summary, setSummary] = useState<AppointmentSummaryResponse | null>(null)
  const [utilisation, setUtilisation] = useState<BranchUtilisationResponse | null>(null)
  const [popularity, setPopularity] = useState<ServicePopularityResponse[]>([])
  const [branches, setBranches] = useState<BranchResponse[] | null>(null)
  const [selectedBranch, setSelectedBranch] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function loadData() {
    if (!from || !to) return
    setLoading(true)
    setError('')
    try {
      let branchList = branches
      if (!branchList) {
        branchList = await getBranches()
        setBranches(branchList)
        if (branchList.length > 0 && !selectedBranch) {
          setSelectedBranch(branchList[0].id)
        }
      }
      const branchIdForUtil = selectedBranch || (branchList[0]?.id ?? '')
      const [sum, pop, util] = await Promise.all([
        getAppointmentSummary(from, to),
        getServicePopularity(from, to),
        branchIdForUtil ? getBranchUtilisation(branchIdForUtil, from, to) : Promise.resolve(null),
      ])
      setSummary(sum)
      setPopularity(pop)
      setUtilisation(util)
    } catch {
      setError('Failed to load analytics data.')
    } finally {
      setLoading(false)
    }
  }

  const statCards = summary
    ? [
        { label: 'Total Booked', value: summary.totalBooked },
        { label: 'Pending', value: summary.totalPending },
        { label: 'Confirmed', value: summary.totalConfirmed },
        { label: 'Completed', value: summary.totalCompleted },
        { label: 'Cancelled', value: summary.totalCancelled },
        { label: 'No-Show', value: summary.totalNoShow },
        { label: 'Rescheduled', value: summary.totalRescheduled },
      ]
    : []

  return (
    <div className="px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-1">Analytics</h1>
      <p className="text-sm text-gray-500 mb-6">Appointment statistics and branch performance</p>

      <div className="flex items-end gap-4 mb-8 bg-white border border-gray-200 rounded-xl p-4">
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">From</label>
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">To</label>
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
        </div>
        {branches && branches.length > 0 && (
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Branch (utilisation)</label>
            <select
              value={selectedBranch}
              onChange={(e) => setSelectedBranch(e.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>{b.name}</option>
              ))}
            </select>
          </div>
        )}
        <button
          onClick={loadData}
          disabled={loading}
          className="bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-5 py-2 rounded-lg transition-colors disabled:opacity-50"
        >
          {loading ? 'Loading…' : 'Load Data'}
        </button>
      </div>

      {error && <p className="text-red-600 text-sm mb-6">{error}</p>}

      {summary && (
        <>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Appointment Summary</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            {statCards.map((s) => (
              <div key={s.label} className="bg-white border border-gray-200 rounded-xl p-4">
                <p className="text-xs text-gray-500 mb-1">{s.label}</p>
                <p className="text-2xl font-bold text-gray-900">{s.value}</p>
              </div>
            ))}
          </div>
        </>
      )}

      {utilisation && (
        <>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Branch Utilisation — {utilisation.branchName}</h2>
          <div className="bg-white border border-gray-200 rounded-xl p-5 mb-8">
            <div className="flex items-center gap-8">
              <div>
                <p className="text-xs text-gray-500 mb-1">Total Slots</p>
                <p className="text-2xl font-bold text-gray-900">{utilisation.totalSlots}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500 mb-1">Booked Slots</p>
                <p className="text-2xl font-bold text-blue-600">{utilisation.bookedSlots}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500 mb-1">Utilisation Rate</p>
                <p className="text-2xl font-bold text-purple-600">
                  {(utilisation.utilisation * 100).toFixed(1)}%
                </p>
              </div>
            </div>
            <div className="mt-4 bg-gray-100 rounded-full h-3 overflow-hidden">
              <div
                className="bg-purple-600 h-3 rounded-full transition-all"
                style={{ width: `${Math.min(utilisation.utilisation * 100, 100)}%` }}
              />
            </div>
          </div>
        </>
      )}

      {popularity.length > 0 && (
        <>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Service Popularity</h2>
          <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 text-xs text-gray-500">
                  <th className="text-left px-5 py-3 font-medium">Rank</th>
                  <th className="text-left px-4 py-3 font-medium">Service</th>
                  <th className="text-left px-4 py-3 font-medium">Total Bookings</th>
                </tr>
              </thead>
              <tbody>
                {popularity.map((s, i) => (
                  <tr key={s.serviceId} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="px-5 py-3 text-gray-400 font-medium">#{i + 1}</td>
                    <td className="px-4 py-3 font-medium text-gray-800">{s.serviceName}</td>
                    <td className="px-4 py-3 text-gray-700">{s.totalBookings}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {!summary && !loading && (
        <div className="text-center py-16 text-gray-400 text-sm">
          Select a date range and click "Load Data" to view analytics.
        </div>
      )}
    </div>
  )
}
