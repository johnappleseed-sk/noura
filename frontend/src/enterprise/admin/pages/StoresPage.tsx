import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { useEffect, useState } from 'react'
import { StoreUpsertRequest } from '@/admin/api/adminApi'
import { useAdminDispatch, useAdminSelector } from '@/admin/app/hooks'
import { createStore, deleteStore, fetchStores, updateStore } from '@/admin/features/stores/storesSlice'
import { Store } from '@/admin/types'

interface StoreFormValues extends Omit<StoreUpsertRequest, 'services'> {
  services: string[]
}

const emptyForm: StoreFormValues = {
  name: '',
  addressLine1: '',
  city: '',
  state: '',
  zipCode: '',
  country: 'USA',
  region: 'US-WEST',
  latitude: 0,
  longitude: 0,
  openTime: '09:00',
  closeTime: '21:00',
  active: true,
  shippingFee: 0,
  freeShippingThreshold: 0,
  services: ['PICKUP', 'DELIVERY'],
}

const serviceOptions = ['PICKUP', 'DELIVERY', 'CURBSIDE', 'B2B_DESK'].map((value) => ({ value, label: value }))

/**
 * Renders the StoresPage component.
 *
 * @returns The rendered component tree.
 */
export const StoresPage = (): JSX.Element => {
  const dispatch = useAdminDispatch()
  const { items, loading, saving } = useAdminSelector((state) => state.stores)
  const [form] = Form.useForm<StoreFormValues>()
  const [editing, setEditing] = useState<Store | null>(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    void dispatch(fetchStores())
  }, [dispatch])

  /**
   * Executes open create.
   *
   * @returns No value.
   */
  const openCreate = (): void => {
    setEditing(null)
    form.setFieldsValue(emptyForm)
    setOpen(true)
  }

  /**
   * Executes open edit.
   *
   * @param store The store value.
   * @returns No value.
   */
  const openEdit = (store: Store): void => {
    setEditing(store)
    form.setFieldsValue({
      name: store.name,
      addressLine1: store.addressLine1,
      city: store.city,
      state: store.state,
      zipCode: store.zipCode,
      country: store.country,
      region: store.region,
      latitude: store.latitude,
      longitude: store.longitude,
      openTime: store.openTime,
      closeTime: store.closeTime,
      active: store.active,
      services: store.services,
      shippingFee: store.shippingFee,
      freeShippingThreshold: store.freeShippingThreshold,
    })
    setOpen(true)
  }

  /**
   * Executes on finish.
   *
   * @param values The values value.
   * @returns No value.
   */
  const onFinish = async (values: StoreFormValues): Promise<void> => {
    const payload: StoreUpsertRequest = {
      ...values,
      services: values.services as StoreUpsertRequest['services'],
    }
    if (editing) {
      await dispatch(updateStore({ id: editing.id, payload }))
      message.success('Store updated')
    } else {
      await dispatch(createStore(payload))
      message.success('Store created')
    }
    setOpen(false)
  }

  /**
   * Executes on delete.
   *
   * @param id The id used to locate the target record.
   * @returns No value.
   */
  const onDelete = async (id: string): Promise<void> => {
    await dispatch(deleteStore(id))
    message.success('Store deleted')
  }

  return (
    <>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Store CRUD
        </Typography.Title>
        <Button type="primary" onClick={openCreate}>
          Add Store
        </Button>
      </Space>
      <Table<Store>
        rowKey="id"
        loading={loading}
        dataSource={items}
        pagination={{ pageSize: 20 }}
        columns={[
          { title: 'Name', dataIndex: 'name' },
          { title: 'City', dataIndex: 'city' },
          { title: 'Region', dataIndex: 'region' },
          { title: 'Open Now', render: (_, record) => (record.openNow ? <Tag color="green">Yes</Tag> : <Tag>No</Tag>) },
          { title: 'Shipping Fee', dataIndex: 'shippingFee', render: (value: number) => `$${value.toFixed(2)}` },
          {
            title: 'Services',
            render: (_, record) => (
              <Space wrap>
                {record.services.map((service) => (
                  <Tag key={service}>{service}</Tag>
                ))}
              </Space>
            ),
          },
          {
            title: 'Actions',
            render: (_, record) => (
              <Space>
                <Button size="small" onClick={() => openEdit(record)}>
                  Edit
                </Button>
                <Popconfirm title="Delete this store?" onConfirm={() => void onDelete(record.id)}>
                  <Button danger size="small">
                    Delete
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal
        destroyOnClose
        open={open}
        title={editing ? 'Edit Store' : 'Create Store'}
        okText={editing ? 'Update' : 'Create'}
        okButtonProps={{ loading: saving }}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
      >
        <Form<StoreFormValues> form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item label="Name" name="name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="Address" name="addressLine1" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="City" name="city" rules={[{ required: true }]} style={{ flex: 1, marginRight: 12 }}>
              <Input />
            </Form.Item>
            <Form.Item label="State" name="state" rules={[{ required: true }]} style={{ flex: 1, marginRight: 12 }}>
              <Input />
            </Form.Item>
            <Form.Item label="ZIP" name="zipCode" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space.Compact>
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="Country" name="country" style={{ flex: 1, marginRight: 12 }}>
              <Input />
            </Form.Item>
            <Form.Item label="Region" name="region" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space.Compact>
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="Latitude" name="latitude" style={{ flex: 1, marginRight: 12 }}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Longitude" name="longitude" style={{ flex: 1 }}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
          </Space.Compact>
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="Open Time" name="openTime" style={{ flex: 1, marginRight: 12 }}>
              <Input placeholder="09:00" />
            </Form.Item>
            <Form.Item label="Close Time" name="closeTime" style={{ flex: 1 }}>
              <Input placeholder="21:00" />
            </Form.Item>
          </Space.Compact>
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="Shipping Fee" name="shippingFee" style={{ flex: 1, marginRight: 12 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Free Shipping Threshold" name="freeShippingThreshold" style={{ flex: 1 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space.Compact>
          <Form.Item label="Services" name="services">
            <Select mode="multiple" options={serviceOptions} />
          </Form.Item>
          <Form.Item label="Active" name="active" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
