import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { App as AntApp, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '@/api/adminApi';
import { PageHeader } from '@/components/PageHeader';
import type { PageResponse } from '@/types/api';
import type { Store, StoreRequest, StoreServiceType } from '@/types/models';

const SERVICE_OPTIONS: StoreServiceType[] = ['PICKUP', 'DELIVERY', 'CURBSIDE', 'B2B_DESK'];

interface StoreFormValues {
  name: string;
  addressLine1: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  region: string;
  latitude: number;
  longitude: number;
  openTime: string;
  closeTime: string;
  active: boolean;
  services: StoreServiceType[];
  shippingFee: number;
  freeShippingThreshold: number;
}

/**
 * Executes to payload.
 *
 * @param values The values value.
 * @returns The result of to payload.
 */
const toPayload = (values: StoreFormValues): StoreRequest => ({
  ...values,
  services: values.services.length > 0 ? values.services : ['PICKUP'],
});

/**
 * Renders the StoresPage component.
 *
 * @returns The rendered component tree.
 */
export const StoresPage = (): JSX.Element => {
  const { message } = AntApp.useApp();
  const [form] = Form.useForm<StoreFormValues>();
  const [pageData, setPageData] = useState<PageResponse<Store> | null>(null);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Store | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await adminApi.getStores({ page: 0, size: 50, sortBy: 'name', direction: 'asc' });
      setPageData(result);
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to load stores');
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void load();
  }, [load]);

  /**
   * Executes open create.
   *
   * @returns No value.
   */
  const openCreate = (): void => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      country: 'United States',
      region: 'US',
      openTime: '09:00',
      closeTime: '21:00',
      active: true,
      services: ['PICKUP', 'DELIVERY'],
      shippingFee: 10,
      freeShippingThreshold: 100,
      latitude: 0,
      longitude: 0,
    });
    setModalOpen(true);
  };

  /**
   * Executes open edit.
   *
   * @param store The store value.
   * @returns No value.
   */
  const openEdit = (store: Store): void => {
    setEditing(store);
    form.setFieldsValue({
      name: store.name,
      addressLine1: store.addressLine1,
      city: store.city,
      state: store.state,
      zipCode: store.zipCode,
      country: store.country,
      region: store.region,
      latitude: Number(store.latitude),
      longitude: Number(store.longitude),
      openTime: store.openTime,
      closeTime: store.closeTime,
      active: store.active,
      services: store.services,
      shippingFee: Number(store.shippingFee),
      freeShippingThreshold: Number(store.freeShippingThreshold),
    });
    setModalOpen(true);
  };

  /**
   * Executes submit.
   *
   * @returns No value.
   */
  const submit = async (): Promise<void> => {
    const values = await form.validateFields();
    const payload = toPayload(values);
    try {
      if (editing) {
        await adminApi.updateStore(editing.id, payload);
        message.success('Store updated');
      } else {
        await adminApi.createStore(payload);
        message.success('Store created');
      }
      setModalOpen(false);
      form.resetFields();
      await load();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to save store');
    }
  };

  /**
   * Removes remove.
   *
   * @param storeId The store id used to locate the target record.
   * @returns No value.
   */
  const remove = async (storeId: string): Promise<void> => {
    try {
      await adminApi.deleteStore(storeId);
      message.success('Store deleted');
      await load();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to delete store');
    }
  };

  return (
    <>
      <PageHeader
        title="Store Management"
        subtitle="Configure location metadata, hours, fulfillment services, and shipping thresholds."
        extra={
          <Button icon={<PlusOutlined />} type="primary" onClick={openCreate}>
            New Store
          </Button>
        }
      />

      <Card className="admin-section-card admin-data-card">
        <Table<Store>
          rowKey="id"
          loading={loading}
          dataSource={pageData?.content ?? []}
          pagination={false}
          scroll={{ x: 840 }}
          columns={[
            { title: 'Name', dataIndex: 'name' },
            {
              title: 'Location',
              render: (_, record) => `${record.city}, ${record.state}`,
            },
            { title: 'Region', dataIndex: 'region' },
            {
              title: 'Status',
              render: (_, record) => (record.active ? 'Active' : 'Inactive'),
            },
            {
              title: 'Actions',
              render: (_, record) => (
                <Space>
                  <Button icon={<EditOutlined />} onClick={() => openEdit(record)}>
                    Edit
                  </Button>
                  <Popconfirm title="Delete store?" onConfirm={() => remove(record.id)}>
                    <Button danger icon={<DeleteOutlined />}>
                      Delete
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title={editing ? 'Edit Store' : 'Create Store'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => {
          void submit();
        }}
        okText={editing ? 'Update' : 'Create'}
        width={760}
      >
        <Form<StoreFormValues> form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="addressLine1" label="Address" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Space style={{ display: 'flex' }} wrap>
            <Form.Item name="city" label="City" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item name="state" label="State" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item name="zipCode" label="ZIP" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} wrap>
            <Form.Item name="country" label="Country" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item name="region" label="Region" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} wrap>
            <Form.Item name="latitude" label="Latitude" rules={[{ required: true }]} style={{ flex: 1 }}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="longitude" label="Longitude" rules={[{ required: true }]} style={{ flex: 1 }}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} wrap>
            <Form.Item name="openTime" label="Open Time (HH:mm)" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item name="closeTime" label="Close Time (HH:mm)" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} wrap>
            <Form.Item name="shippingFee" label="Shipping Fee" rules={[{ required: true }]} style={{ flex: 1 }}>
              <InputNumber style={{ width: '100%' }} min={0} precision={2} />
            </Form.Item>
            <Form.Item
              name="freeShippingThreshold"
              label="Free Shipping Threshold"
              rules={[{ required: true }]}
              style={{ flex: 1 }}
            >
              <InputNumber style={{ width: '100%' }} min={0} precision={2} />
            </Form.Item>
          </Space>
          <Form.Item name="services" label="Services" rules={[{ required: true }]}>
            <Select mode="multiple" options={SERVICE_OPTIONS.map((service) => ({ label: service, value: service }))} />
          </Form.Item>
          <Form.Item name="active" label="Active" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
