import { useEffect, useState } from 'react'
import { listAuditLogs } from '../../api/admin'
import type { AuditLogResponse, PageResponse } from '../../types'

export default function AdminAuditPage() {
  const [page, setPage] = useState<PageResponse<AuditLogResponse> | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    listAuditLogs(currentPage, 20)
      .then(setPage)
      .catch(() => setError('Failed to load audit logs.'))
      .finally(() => setLoading(false))
  }, [currentPage])

  const logs = page?.content ?? []

  return (
    <div className="px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-1">Audit Logs</h1>
      <p className="text-sm text-gray-500 mb-6">System activity and admin actions</p>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3 mb-4">
          {error}
        </div>
      )}

      <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16">
            <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : logs.length === 0 ? (
          <div className="px-5 py-12 text-center text-sm text-gray-400">No audit log entries.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 text-xs text-gray-500">
                  <th className="text-left px-5 py-3 font-medium">Timestamp</th>
                  <th className="text-left px-4 py-3 font-medium">Action</th>
                  <th className="text-left px-4 py-3 font-medium">Entity</th>
                  <th className="text-left px-4 py-3 font-medium">Entity ID</th>
                  <th className="text-left px-4 py-3 font-medium">Details</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="px-5 py-3 text-xs text-gray-400 whitespace-nowrap">
                      {new Date(log.createdAt).toLocaleString('en-ZA')}
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-700 font-mono">
                        {log.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500">{log.entityType ?? '—'}</td>
                    <td className="px-4 py-3 font-mono text-xs text-gray-400 max-w-[10rem] truncate">
                      {log.entityId ?? '—'}
                    </td>
                    <td className="px-4 py-3 text-gray-500 max-w-xs truncate" title={log.details ?? ''}>
                      {log.details ?? '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {page && page.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-6">
          <button
            disabled={currentPage === 0}
            onClick={() => setCurrentPage((p) => p - 1)}
            className="px-4 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50"
          >
            Previous
          </button>
          <span className="text-sm text-gray-500">
            Page {currentPage + 1} of {page.totalPages}
          </span>
          <button
            disabled={currentPage >= page.totalPages - 1}
            onClick={() => setCurrentPage((p) => p + 1)}
            className="px-4 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
