import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getCompany, createCompany, updateCompany, getCompanyContacts, addContact } from '../features/b2bApi';

const COMPANY_TYPES = ['BUSINESS', 'GOVERNMENT', 'NONPROFIT', 'EDUCATION', 'HEALTHCARE'];
const STATUSES = ['PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED', 'CLOSED'];

export default function CompanyDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isNew = id === 'new';

  const [company, setCompany] = useState({
    name: '',
    legalName: '',
    type: 'BUSINESS',
    status: 'PENDING_APPROVAL',
    taxId: '',
    creditLimit: 0,
    paymentTermsDays: 30,
    discountPercent: 0,
    email: '',
    phone: '',
    website: '',
    billingAddressLine1: '',
    billingCity: '',
    billingState: '',
    billingPostalCode: '',
    billingCountryCode: 'US',
    notes: ''
  });
  const [contacts, setContacts] = useState([]);
  const [newContact, setNewContact] = useState({ name: '', email: '', phone: '', title: '', isPrimary: false });
  const [loading, setLoading] = useState(!isNew);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [showContactForm, setShowContactForm] = useState(false);

  useEffect(() => {
    if (!isNew) {
      loadCompany();
      loadContacts();
    }
  }, [id]);

  const loadCompany = async () => {
    try {
      const response = await getCompany(id);
      setCompany(response.data);
    } catch (err) {
      setError('Failed to load company');
    } finally {
      setLoading(false);
    }
  };

  const loadContacts = async () => {
    try {
      const response = await getCompanyContacts(id);
      setContacts(response.data);
    } catch (err) {
      console.error('Failed to load contacts');
    }
  };

  const handleChange = (e) => {
    const { name, value, type } = e.target;
    setCompany(prev => ({
      ...prev,
      [name]: type === 'number' ? parseFloat(value) || 0 : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    try {
      if (isNew) {
        const response = await createCompany(company);
        navigate(`/admin/b2b/companies/${response.data.id}`);
      } else {
        await updateCompany(id, company);
        navigate('/admin/b2b/companies');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save company');
    } finally {
      setSaving(false);
    }
  };

  const handleAddContact = async (e) => {
    e.preventDefault();
    try {
      await addContact(id, newContact);
      setNewContact({ name: '', email: '', phone: '', title: '', isPrimary: false });
      setShowContactForm(false);
      loadContacts();
    } catch (err) {
      setError('Failed to add contact');
    }
  };

  if (loading) return <div className="p-6">Loading...</div>;

  return (
    <div className="p-6 max-w-4xl">
      <h1 className="text-2xl font-bold mb-6">{isNew ? 'Create Company' : 'Edit Company'}</h1>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">{error}</div>
      )}

      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Company Name *</label>
            <input
              type="text"
              name="name"
              value={company.name}
              onChange={handleChange}
              required
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Legal Name</label>
            <input
              type="text"
              name="legalName"
              value={company.legalName || ''}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
        </div>

        <div className="grid grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
            <select
              name="type"
              value={company.type}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            >
              {COMPANY_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <select
              name="status"
              value={company.status}
              onChange={handleChange}
              disabled={isNew}
              className="w-full border rounded px-3 py-2"
            >
              {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tax ID</label>
            <input
              type="text"
              name="taxId"
              value={company.taxId || ''}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
        </div>

        <hr />

        <h3 className="text-lg font-medium">Credit & Pricing</h3>
        <div className="grid grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Credit Limit ($)</label>
            <input
              type="number"
              name="creditLimit"
              value={company.creditLimit || 0}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Payment Terms (Days)</label>
            <input
              type="number"
              name="paymentTermsDays"
              value={company.paymentTermsDays || 30}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Discount %</label>
            <input
              type="number"
              step="0.01"
              name="discountPercent"
              value={company.discountPercent || 0}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
        </div>

        <hr />

        <h3 className="text-lg font-medium">Contact Information</h3>
        <div className="grid grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              type="email"
              name="email"
              value={company.email || ''}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
            <input
              type="text"
              name="phone"
              value={company.phone || ''}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Website</label>
            <input
              type="url"
              name="website"
              value={company.website || ''}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
        </div>

        <hr />

        <h3 className="text-lg font-medium">Billing Address</h3>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Address Line 1</label>
          <input
            type="text"
            name="billingAddressLine1"
            value={company.billingAddressLine1 || ''}
            onChange={handleChange}
            className="w-full border rounded px-3 py-2"
          />
        </div>
        <div className="grid grid-cols-4 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
            <input
              type="text"
              name="billingCity"
              value={company.billingCity || ''}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">State</label>
            <input
              type="text"
              name="billingState"
              value={company.billingState || ''}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Postal Code</label>
            <input
              type="text"
              name="billingPostalCode"
              value={company.billingPostalCode || ''}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Country</label>
            <input
              type="text"
              name="billingCountryCode"
              value={company.billingCountryCode || 'US'}
              onChange={handleChange}
              className="w-full border rounded px-3 py-2"
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
          <textarea
            name="notes"
            value={company.notes || ''}
            onChange={handleChange}
            rows={3}
            className="w-full border rounded px-3 py-2"
          />
        </div>

        <div className="flex gap-4 pt-4">
          <button
            type="submit"
            disabled={saving}
            className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/admin/b2b/companies')}
            className="border border-gray-300 px-6 py-2 rounded hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </form>

      {/* Contacts Section */}
      {!isNew && (
        <div className="mt-8 bg-white rounded-lg shadow p-6">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-bold">Contacts</h2>
            <button
              onClick={() => setShowContactForm(!showContactForm)}
              className="text-blue-600 hover:underline"
            >
              {showContactForm ? 'Cancel' : '+ Add Contact'}
            </button>
          </div>

          {showContactForm && (
            <form onSubmit={handleAddContact} className="mb-4 p-4 bg-gray-50 rounded space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <input
                  type="text"
                  placeholder="Name"
                  value={newContact.name}
                  onChange={(e) => setNewContact(prev => ({ ...prev, name: e.target.value }))}
                  required
                  className="border rounded px-3 py-2"
                />
                <input
                  type="text"
                  placeholder="Title"
                  value={newContact.title}
                  onChange={(e) => setNewContact(prev => ({ ...prev, title: e.target.value }))}
                  className="border rounded px-3 py-2"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <input
                  type="email"
                  placeholder="Email"
                  value={newContact.email}
                  onChange={(e) => setNewContact(prev => ({ ...prev, email: e.target.value }))}
                  className="border rounded px-3 py-2"
                />
                <input
                  type="text"
                  placeholder="Phone"
                  value={newContact.phone}
                  onChange={(e) => setNewContact(prev => ({ ...prev, phone: e.target.value }))}
                  className="border rounded px-3 py-2"
                />
              </div>
              <div className="flex items-center gap-4">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={newContact.isPrimary}
                    onChange={(e) => setNewContact(prev => ({ ...prev, isPrimary: e.target.checked }))}
                  />
                  Primary Contact
                </label>
                <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
                  Add
                </button>
              </div>
            </form>
          )}

          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Title</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Phone</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Primary</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {contacts.map((contact) => (
                <tr key={contact.id}>
                  <td className="px-4 py-2">{contact.name}</td>
                  <td className="px-4 py-2 text-sm text-gray-500">{contact.title}</td>
                  <td className="px-4 py-2 text-sm">{contact.email}</td>
                  <td className="px-4 py-2 text-sm">{contact.phone}</td>
                  <td className="px-4 py-2">
                    {contact.isPrimary && <span className="text-green-600">✓</span>}
                  </td>
                </tr>
              ))}
              {contacts.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-4 text-center text-gray-500">No contacts</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
