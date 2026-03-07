import { Button, Form, Input, InputNumber, Modal, Popconfirm, Space, Switch, Table, Tag, Typography, message } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useAdminDispatch, useAdminSelector } from '@/admin/app/hooks'
import { ProductUpsertRequest } from '@/admin/api/adminApi'
import { createProduct, deleteProduct, fetchProducts, updateProduct } from '@/admin/features/products/productsSlice'
import { fetchStores } from '@/admin/features/stores/storesSlice'
import { Product } from '@/admin/types'

interface ProductFormValues {
  name: string
  category: string
  brand: string
  price: number
  flashSale: boolean
  trending: boolean
  bestSeller: boolean
  shortDescription: string
  longDescription: string
  seoTitle: string
  seoDescription: string
  seoSlug: string
  color: string
  size: string
  sku: string
  imageUrl: string
  videoUrl: string
  storeId: string
  storeStock: number
  storePrice: number
}

const emptyForm: ProductFormValues = {
  name: '',
  category: '',
  brand: '',
  price: 0,
  flashSale: false,
  trending: false,
  bestSeller: false,
  shortDescription: '',
  longDescription: '',
  seoTitle: '',
  seoDescription: '',
  seoSlug: '',
  color: '',
  size: '',
  sku: '',
  imageUrl: '',
  videoUrl: '',
  storeId: '',
  storeStock: 0,
  storePrice: 0,
}

/**
 * Executes to payload.
 *
 * @param values The values value.
 * @returns The result of to payload.
 */
const toPayload = (values: ProductFormValues): ProductUpsertRequest => ({
  name: values.name,
  category: values.category,
  brand: values.brand,
  price: values.price,
  flashSale: values.flashSale,
  trending: values.trending,
  bestSeller: values.bestSeller,
  shortDescription: values.shortDescription,
  longDescription: values.longDescription,
  seoTitle: values.seoTitle,
  seoDescription: values.seoDescription,
  seoSlug: values.seoSlug,
  variants: values.sku
    ? [
        {
          color: values.color || 'Default',
          size: values.size || 'Standard',
          sku: values.sku,
        },
      ]
    : [],
  media: [
    ...(values.imageUrl ? [{ mediaType: 'IMAGE', url: values.imageUrl, sortOrder: 1 }] : []),
    ...(values.videoUrl ? [{ mediaType: 'VIDEO', url: values.videoUrl, sortOrder: 2 }] : []),
  ],
  inventory: values.storeId
    ? [
        {
          storeId: values.storeId,
          stock: values.storeStock,
          storePrice: values.storePrice || values.price,
        },
      ]
    : [],
})

/**
 * Renders the ProductsPage component.
 *
 * @returns The rendered component tree.
 */
export const ProductsPage = (): JSX.Element => {
  const dispatch = useAdminDispatch()
  const { items, loading, saving } = useAdminSelector((state) => state.products)
  const stores = useAdminSelector((state) => state.stores.items)
  const [form] = Form.useForm<ProductFormValues>()
  const [editing, setEditing] = useState<Product | null>(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    void dispatch(fetchProducts({ page: 0, size: 20, sortBy: 'createdAt', direction: 'desc' }))
    void dispatch(fetchStores())
  }, [dispatch])

  const storeOptions = useMemo(
    () =>
      stores.map((store) => ({
        label: `${store.name} (${store.city})`,
        value: store.id,
      })),
    [stores],
  )

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
   * @param product The product value.
   * @returns No value.
   */
  const openEdit = (product: Product): void => {
    setEditing(product)
    const variant = product.variants[0]
    const image = product.media.find((entry) => entry.mediaType.toUpperCase() === 'IMAGE')
    const video = product.media.find((entry) => entry.mediaType.toUpperCase() === 'VIDEO')
    const inventory = product.storeInventory[0]
    form.setFieldsValue({
      name: product.name,
      category: product.category,
      brand: product.brand,
      price: product.price,
      flashSale: product.flashSale,
      trending: product.trending,
      bestSeller: product.bestSeller,
      shortDescription: product.shortDescription ?? '',
      longDescription: product.longDescription ?? '',
      seoTitle: product.seoTitle ?? '',
      seoDescription: product.seoDescription ?? '',
      seoSlug: product.seoSlug ?? '',
      color: variant?.color ?? '',
      size: variant?.size ?? '',
      sku: variant?.sku ?? '',
      imageUrl: image?.url ?? '',
      videoUrl: video?.url ?? '',
      storeId: inventory?.storeId ?? '',
      storeStock: inventory?.stock ?? 0,
      storePrice: inventory?.storePrice ?? product.price,
    })
    setOpen(true)
  }

  /**
   * Executes on finish.
   *
   * @param values The values value.
   * @returns No value.
   */
  const onFinish = async (values: ProductFormValues): Promise<void> => {
    const payload = toPayload(values)
    if (editing) {
      await dispatch(updateProduct({ id: editing.id, payload }))
      message.success('Product updated')
    } else {
      await dispatch(createProduct(payload))
      message.success('Product created')
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
    await dispatch(deleteProduct(id))
    message.success('Product deleted')
  }

  return (
    <>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          Product CRUD
        </Typography.Title>
        <Button type="primary" onClick={openCreate}>
          Add Product
        </Button>
      </Space>
      <Table<Product>
        loading={loading}
        dataSource={items}
        rowKey="id"
        pagination={{ pageSize: 20 }}
        columns={[
          { title: 'Name', dataIndex: 'name' },
          { title: 'Category', dataIndex: 'category' },
          { title: 'Brand', dataIndex: 'brand' },
          { title: 'Price', dataIndex: 'price', render: (value: number) => `$${value.toFixed(2)}` },
          {
            title: 'Flags',
            render: (_, record) => (
              <Space>
                {record.flashSale ? <Tag color="gold">Flash Sale</Tag> : null}
                {record.trending ? <Tag color="blue">Trending</Tag> : null}
                {record.bestSeller ? <Tag color="green">Best Seller</Tag> : null}
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
                <Popconfirm title="Delete this product?" onConfirm={() => void onDelete(record.id)}>
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
        title={editing ? 'Edit Product' : 'Create Product'}
        width={760}
        okText={editing ? 'Update' : 'Create'}
        okButtonProps={{ loading: saving }}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
      >
        <Form<ProductFormValues> form={form} layout="vertical" onFinish={onFinish}>
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="Name" name="name" rules={[{ required: true }]} style={{ flex: 1, marginRight: 12 }}>
              <Input />
            </Form.Item>
            <Form.Item label="Category" name="category" rules={[{ required: true }]} style={{ flex: 1, marginRight: 12 }}>
              <Input />
            </Form.Item>
            <Form.Item label="Brand" name="brand" rules={[{ required: true }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space.Compact>

          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="Base Price" name="price" rules={[{ required: true }]} style={{ flex: 1, marginRight: 12 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Store" name="storeId" style={{ flex: 1, marginRight: 12 }}>
              <Input list="stores-list" placeholder="Store ID" />
            </Form.Item>
            <Form.Item label="Store Stock" name="storeStock" style={{ flex: 1, marginRight: 12 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Store Price" name="storePrice" style={{ flex: 1 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space.Compact>

          <datalist id="stores-list">
            {storeOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </datalist>

          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="SKU" name="sku" style={{ flex: 1, marginRight: 12 }}>
              <Input />
            </Form.Item>
            <Form.Item label="Color" name="color" style={{ flex: 1, marginRight: 12 }}>
              <Input />
            </Form.Item>
            <Form.Item label="Size" name="size" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space.Compact>

          <Form.Item label="Image URL" name="imageUrl">
            <Input />
          </Form.Item>
          <Form.Item label="Video URL" name="videoUrl">
            <Input />
          </Form.Item>
          <Form.Item label="Short Description" name="shortDescription">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="Long Description" name="longDescription">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="SEO Title" name="seoTitle" style={{ flex: 1, marginRight: 12 }}>
              <Input />
            </Form.Item>
            <Form.Item label="SEO Slug" name="seoSlug" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space.Compact>
          <Form.Item label="SEO Description" name="seoDescription">
            <Input />
          </Form.Item>
          <Space size={24}>
            <Form.Item label="Flash Sale" name="flashSale" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="Trending" name="trending" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="Best Seller" name="bestSeller" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </>
  )
}
