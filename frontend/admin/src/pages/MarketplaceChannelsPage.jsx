import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getChannels, testChannel, createChannel } from '../features/marketplaceApi';

const CHANNEL_TYPES = ['AMAZON', 'EBAY', 'SHOPIFY', 'WALMART', 'WOOCOMMERCE', 'ETSY', 'CUSTOM'];

export default function MarketplaceChannelsPage() {
  const [channels, setChannels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [testing, setTesting] = useState(null);
  const [testResults, setTestResults] = useState({});
  const [newChannel, setNewChannel] = useState({
    code: '',
    name: '',
    type: 'SHOPIFY',
    apiKey: '',
    apiSecret: '',
    accessToken: '',
    merchantId: '',
    apiEndpoint: ''
  });

  useEffect(() => {
    loadChannels();
  }, []);

  const loadChannels = async () => {
    try {
      setLoading(true);
      const response = await getChannels();
      setChannels(response.data);
    } catch (err) {
      setError('Failed to load channels');
    } finally {
      setLoading(false);
    }
  };

  const handleTest = async (id) => {
    setTesting(id);
    try {
      const response = await testChannel(id);
      setTestResults(prev => ({ ...prev, [id]: response.data }));
    } catch (err) {
      setTestResults(prev => ({ ...prev, [id]: { success: false, message: 'Test failed' } }));
    } finally {
      setTesting(null);
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await createChannel(newChannel);
      setShowForm(false);
      setNewChannel({ code: '', name: '', type: 'SHOPIFY', apiKey: '', apiSecret: '', accessToken: '', merchantId: '', apiEndpoint: '' });
      loadChannels();
    } catch (err) {
      setError('Failed to create channel');
    }
  };

  const typeBadge = (type) => {
    const colors = {
      AMAZON: 'bg-orange-100 text-orange-800',
      EBAY: 'bg-blue-100 text-blue-800',
      SHOPIFY: 'bg-green-100 text-green-800',
      WALMART: 'bg-blue-100 text-blue-800'
    };
    return <span className={`px-2 py-1 text-xs rounded ${colors[type] || 'bg-gray-100'}`}>{type}</span>;
  };

  if (loading) return <div className="p-6">Loading...</div>;

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Marketplace Channels</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          {showForm ? 'Cancel' : 'Add Channel'}
        </button>
      </div>

      {error && <div className="bg-red-100 text-red-700 p-3 rounded mb-4">{error}</div>}

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white rounded-lg shadow p-6 mb-6 space-y-4">
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Code *</label>
              <input
                type="text"
                value={newChannel.code}
                onChange={(e) => setNewChannel(prev => ({ ...prev, code: e.target.value }))}
                required
                className="w-full border rounded px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name *</label>
              <input
                type="text"
                value={newChannel.name}
                onChange={(e) => setNewChannel(prev => ({ ...prev, name: e.target.value }))}
                required
                className="w-full border rounded px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
              <select
                value={newChannel.type}
                onChange={(e) => setNewChannel(prev => ({ ...prev, type: e.target.value }))}
                className="w-full border rounded px-3 py-2"
              >
                {CHANNEL_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">API Key</label>
              <input
                type="password"
                value={newChannel.apiKey}
                onChange={(e) => setNewChannel(prev => ({ ...prev, apiKey: e.target.value }))}
                className="w-full border rounded px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">API Secret</label>
              <input
                type="password"
                value={newChannel.apiSecret}
                onChange={(e) => setNewChannel(prev => ({ ...prev, apiSecret: e.target.value }))}
                className="w-full border rounded px-3 py-2"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Access Token</label>
              <input
                type="password"
                value={newChannel.accessToken}
                onChange={(e) => setNewChannel(prev => ({ ...prev, accessToken: e.target.value }))}
                className="w-full border rounded px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Merchant ID</label>
              <input
                type="text"
                value={newChannel.merchantId}
                onChange={(e) => setNewChannel(prev => ({ ...prev, merchantId: e.target.value }))}
                className="w-full border rounded px-3 py-2"
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">API Endpoint</label>
            <input
              type="url"
              value={newChannel.apiEndpoint}
              onChange={(e) => setNewChannel(prev => ({ ...prev, apiEndpoint: e.target.value }))}
              placeholder="https://..."
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <button type="submit" className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700">
            Create Channel
          </button>
        </form>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Code</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Last Sync</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {channels.map((channel) => (
              <tr key={channel.id}>
                <td className="px-6 py-4 whitespace-nowrap font-mono text-sm">{channel.code}</td>
                <td className="px-6 py-4 whitespace-nowrap">{channel.name}</td>
                <td className="px-6 py-4 whitespace-nowrap">{typeBadge(channel.type)}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 text-xs rounded ${
                    channel.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  }`}>
                    {channel.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {channel.lastOrderSync ? new Date(channel.lastOrderSync).toLocaleDateString() : '—'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                  <button
                    onClick={() => handleTest(channel.id)}
                    disabled={testing === channel.id}
                    className="text-blue-600 hover:underline disabled:opacity-50"
                  >
                    {testing === channel.id ? 'Testing...' : 'Test'}
                  </button>
                  <Link to={`/admin/marketplace/channels/${channel.id}/mappings`} className="text-green-600 hover:underline">
                    Mappings
                  </Link>
                  <Link to={`/admin/marketplace/channels/${channel.id}/orders`} className="text-purple-600 hover:underline">
                    Orders
                  </Link>
                </td>
              </tr>
            ))}
            {channels.length === 0 && (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-gray-500">
                  No marketplace channels configured. Click "Add Channel" to get started.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Test Results */}
      {Object.keys(testResults).length > 0 && (
        <div className="mt-4 space-y-2">
          {Object.entries(testResults).map(([id, result]) => (
            <div key={id} className={`p-3 rounded ${result.success ? 'bg-green-100' : 'bg-red-100'}`}>
              Channel {id}: {result.success ? '✓' : '✗'} {result.message}
              {result.sellerName && ` (${result.sellerName})`}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
