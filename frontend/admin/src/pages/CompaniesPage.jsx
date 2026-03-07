import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getCompanies, approveCompany, suspendCompany } from '../features/b2bApi';

const STATUSES = ['', 'PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED', 'CLOSED'];

export default function CompaniesPage() {
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    loadCompanies();
  }, [status, page]);

  const loadCompanies = async () => {
    try {
      setLoading(true);
      const params = { page, size: 20 };
      if (status) params.status = status;
      const response = await getCompanies(params);
      setCompanies(response.data.content);
      setTotalPages(response.data.totalPages);
    } catch (err) {
      setError('Failed to load companies');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    try {
      await approveCompany(id);
      loadCompanies();
    } catch (err) {
      setError('Failed to approve company');
    }
  };

  const handleSuspend = async (id) => {
    if (!confirm('Are you sure you want to suspend this company?')) return;
    try {
      await suspendCompany(id);
      loadCompanies();
    } catch (err) {
      setError('Failed to suspend company');
    }
  };

  const statusBadge = (s) => {
    const colors = {
      PENDING_APPROVAL: 'bg-yellow-100 text-yellow-800',
      ACTIVE: 'bg-green-100 text-green-800',
      SUSPENDED: 'bg-red-100 text-red-800',
      CLOSED: 'bg-gray-100 text-gray-800'
    };
    return <span className={`px-2 py-1 text-xs rounded ${colors[s] || 'bg-gray-100'}`}>{s}</span>;
  };

  if (loading && page === 0) return <div className="p-6">Loading...</div>;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">B2B Companies</h1>
        <Link
          to="/admin/b2b/companies/new"
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          Add Company
        </Link>
      </div>

      {error && <div className="bg-red-100 text-red-700 p-3 rounded mb-4">{error}</div>}

      <div className="mb-4">
        <select
          value={status}
          onChange={(e) => { setStatus(e.target.value); setPage(0); }}
          className="border rounded px-3 py-2"
        >
          <option value="">All Statuses</option>
          {STATUSES.filter(s => s).map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Credit Limit</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Balance</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {companies.map((company) => (
              <tr key={company.id}>
                <td className="px-6 py-4 whitespace-nowrap">
                  <Link to={`/admin/b2b/companies/${company.id}`} className="text-blue-600 hover:underline">
                    {company.name}
                  </Link>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">{company.type}</td>
                <td className="px-6 py-4 whitespace-nowrap">{statusBadge(company.status)}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  ${company.creditLimit?.toLocaleString() || '—'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  ${company.currentBalance?.toLocaleString() || '0'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                  <Link to={`/admin/b2b/companies/${company.id}`} className="text-blue-600 hover:underline">
                    Edit
                  </Link>
                  {company.status === 'PENDING_APPROVAL' && (
                    <button onClick={() => handleApprove(company.id)} className="text-green-600 hover:underline">
                      Approve
                    </button>
                  )}
                  {company.status === 'ACTIVE' && (
                    <button onClick={() => handleSuspend(company.id)} className="text-red-600 hover:underline">
                      Suspend
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-4">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-3 py-1 border rounded disabled:opacity-50"
          >
            Prev
          </button>
          <span className="px-3 py-1">Page {page + 1} of {totalPages}</span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="px-3 py-1 border rounded disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
