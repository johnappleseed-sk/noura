import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import {
  App as AntApp,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Typography,
} from 'antd';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { adminApi } from '@/api/adminApi';
import { PageHeader } from '@/components/PageHeader';
import type { PageResponse } from '@/types/api';
import type { Product, ProductRequest } from '@/types/models';

interface ProductFormValues {
  name: string;
  categoryId?: string;
  category?: string;
  brand?: string;
  price: number;
  attributesJson: string;
  allowBackorder: boolean;
  flashSale: boolean;
  trending: boolean;
  bestSeller: boolean;
  shortDescription?: string;
  longDescription?: string;
  seoTitle?: string;
  seoDescription?: string;
  seoSlug?: string;
}

type ProductFilter = 'ALL' | 'FLASH_SALE' | 'TRENDING' | 'BEST_SELLER';
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/**
 * Parses attributes JSON.
 *
 * @param raw The raw value.
 * @returns Parsed JSON object.
 */
const parseAttributesJson = (raw: string): Record<string, unknown> => {
  const source = raw.trim();
  if (source.length === 0) {
    return {};
  }
  const parsed = JSON.parse(source) as unknown;
  if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('Attributes must be a valid JSON object');
  }
  return parsed as Record<string, unknown>;
};

/**
 * Executes to payload.
 *
 * @param values The values value.
 * @returns The result of to payload.
 */
const toPayload = (values: ProductFormValues): ProductRequest => ({
  name: values.name.trim(),
  description: values.shortDescription?.trim() || undefined,
  categoryId: values.categoryId?.trim() || undefined,
  category: values.category?.trim() || (values.categoryId?.trim() ? undefined : 'General'),
  brand: values.brand?.trim() || undefined,
  price: values.price,
  attributes: parseAttributesJson(values.attributesJson),
  allowBackorder: values.allowBackorder,
  flashSale: values.flashSale,
  trending: values.trending,
  bestSeller: values.bestSeller,
  shortDescription: values.shortDescription?.trim() || undefined,
  longDescription: values.longDescription?.trim() || undefined,
  seoTitle: values.seoTitle?.trim() || undefined,
  seoDescription: values.seoDescription?.trim() || undefined,
  seoSlug:
    values.seoSlug?.trim() ||
    values.name
      .toLowerCase()
      .trim()
      .replace(/\s+/g, '-'),
  variants: [],
  media: [],
  inventory: [],
});

/**
 * Renders the ProductsPage component.
 *
 * @returns The rendered component tree.
 */
export const ProductsPage = (): JSX.Element => {
  const { message } = AntApp.useApp();
  const [form] = Form.useForm<ProductFormValues>();
  const [pageData, setPageData] = useState<PageResponse<Product> | null>(null);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [productFilter, setProductFilter] = useState<ProductFilter>('ALL');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await adminApi.getProducts({ page: 0, size: 50, sortBy: 'createdAt', direction: 'desc' });
      setPageData(result);
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to load products');
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
      categoryId: undefined,
      category: undefined,
      brand: undefined,
      flashSale: false,
      trending: false,
      bestSeller: false,
      allowBackorder: false,
      price: 10,
      attributesJson: '{}',
      shortDescription: undefined,
      longDescription: undefined,
      seoTitle: undefined,
      seoDescription: undefined,
      seoSlug: undefined,
    });
    setModalOpen(true);
  };

  /**
   * Executes open edit.
   *
   * @param record The record value.
   * @returns No value.
   */
  const openEdit = (record: Product): void => {
    setEditing(record);
    form.setFieldsValue({
      name: record.name,
      categoryId: undefined,
      category: record.category,
      brand: record.brand,
      price: Number(record.price),
      attributesJson: JSON.stringify(record.attributes ?? {}, null, 2),
      allowBackorder: record.allowBackorder ?? false,
      flashSale: record.flashSale,
      trending: record.trending,
      bestSeller: record.bestSeller,
      shortDescription: record.shortDescription,
      longDescription: record.longDescription,
      seoTitle: record.seoTitle,
      seoDescription: record.seoDescription,
      seoSlug: record.seoSlug,
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
    let payload: ProductRequest;
    try {
      payload = toPayload(values);
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Invalid attributes JSON');
      return;
    }
    try {
      if (editing) {
        await adminApi.updateProduct(editing.id, payload);
        message.success('Product updated');
      } else {
        await adminApi.createProduct(payload);
        message.success('Product created');
      }
      setModalOpen(false);
      form.resetFields();
      await load();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to save product');
    }
  };

  /**
   * Removes remove.
   *
   * @param productId The product id used to locate the target record.
   * @returns No value.
   */
  const remove = async (productId: string): Promise<void> => {
    try {
      await adminApi.deleteProduct(productId);
      message.success('Product deleted');
      await load();
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Failed to delete product');
    }
  };

  const products = pageData?.content ?? [];

  const filteredProducts = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();

    return products.filter((product) => {
      const matchesKeyword =
        keyword.length === 0 ||
        product.name.toLowerCase().includes(keyword) ||
        (product.category ?? '').toLowerCase().includes(keyword) ||
        (product.brand ?? '').toLowerCase().includes(keyword);

      const matchesFilter =
        productFilter === 'ALL' ||
        (productFilter === 'FLASH_SALE' && product.flashSale) ||
        (productFilter === 'TRENDING' && product.trending) ||
        (productFilter === 'BEST_SELLER' && product.bestSeller);

      return matchesKeyword && matchesFilter;
    });
  }, [productFilter, products, searchTerm]);

  const featuredCount = useMemo(
    () => filteredProducts.filter((product) => product.flashSale || product.trending || product.bestSeller).length,
    [filteredProducts],
  );

  const averagePrice = useMemo(() => {
    if (filteredProducts.length === 0) {
      return 0;
    }
    return filteredProducts.reduce((total, product) => total + Number(product.price ?? 0), 0) / filteredProducts.length;
  }, [filteredProducts]);

  const averageRating = useMemo(() => {
    if (filteredProducts.length === 0) {
      return 0;
    }
    return filteredProducts.reduce((total, product) => total + Number(product.averageRating ?? 0), 0) / filteredProducts.length;
  }, [filteredProducts]);

  return (
    <>
      <PageHeader
        title="Product Management"
        subtitle="Create, update, and retire catalog items with clear merchandising metadata."
        extra={
          <Button icon={<PlusOutlined />} type="primary" onClick={openCreate}>
            New Product
          </Button>
        }
      />

      <Card className="admin-section-card">
        <Space wrap size={[16, 16]} style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space wrap size={[12, 12]}>
            <Statistic title="Visible Products" value={filteredProducts.length} />
            <Statistic title="Featured" value={featuredCount} />
            <Statistic title="Avg. Price" precision={2} prefix="$" value={averagePrice} />
            <Statistic title="Avg. Rating" precision={1} value={averageRating} />
          </Space>

          <Space wrap>
            <Input.Search
              placeholder="Search by name, category, or brand"
              allowClear
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              style={{ width: 320, maxWidth: '100%' }}
            />
            <Select<ProductFilter>
              value={productFilter}
              style={{ width: 180 }}
              options={[
                { label: 'All Products', value: 'ALL' },
                { label: 'Flash Sale', value: 'FLASH_SALE' },
                { label: 'Trending', value: 'TRENDING' },
                { label: 'Best Seller', value: 'BEST_SELLER' },
              ]}
              onChange={setProductFilter}
            />
            <Button
              onClick={() => {
                setSearchTerm('');
                setProductFilter('ALL');
              }}
            >
              Reset
            </Button>
          </Space>
        </Space>
      </Card>

      <Card className="admin-section-card admin-data-card">
        <Table<Product>
          rowKey="id"
          loading={loading}
          dataSource={filteredProducts}
          pagination={false}
          scroll={{ x: 1080 }}
          columns={[
            { title: 'Name', dataIndex: 'name' },
            { title: 'Category', dataIndex: 'category' },
            { title: 'Brand', dataIndex: 'brand' },
            { title: 'Price', dataIndex: 'price', render: (value: number) => `$${Number(value).toFixed(2)}` },
            { title: 'Rating', dataIndex: 'averageRating', render: (value: number) => Number(value ?? 0).toFixed(1) },
            {
              title: 'State',
              render: (_, record) => (
                <Space size={[6, 6]} wrap>
                  <Tag color={record.active ? 'green' : 'default'}>{record.active ? 'Active' : 'Inactive'}</Tag>
                  {record.status ? <Tag color="blue">{record.status}</Tag> : null}
                  {record.allowBackorder ? <Tag color="orange">Backorder</Tag> : null}
                </Space>
              ),
            },
            {
              title: 'Signals',
              width: 220,
              render: (_, record) => (
                <Space size={[6, 6]} wrap>
                  {record.flashSale ? <Tag color="red">Flash Sale</Tag> : null}
                  {record.trending ? <Tag color="gold">Trending</Tag> : null}
                  {record.bestSeller ? <Tag color="green">Best Seller</Tag> : null}
                  {!record.flashSale && !record.trending && !record.bestSeller ? <Tag>Standard</Tag> : null}
                </Space>
              ),
            },
            {
              title: 'Actions',
              render: (_, record) => (
                <Space>
                  <Button icon={<EditOutlined />} onClick={() => openEdit(record)}>
                    Edit
                  </Button>
                  <Popconfirm title="Delete product?" onConfirm={() => remove(record.id)}>
                    <Button danger icon={<DeleteOutlined />}>
                      Delete
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
        <Typography.Paragraph type="secondary" style={{ margin: '12px 16px' }}>
          Total products in catalog: {pageData?.totalElements ?? 0}
        </Typography.Paragraph>
      </Card>

      <Modal
        title={editing ? 'Edit Product' : 'Create Product'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => {
          void submit();
        }}
        okText={editing ? 'Update' : 'Create'}
        width={760}
      >
        <Form<ProductFormValues> layout="vertical" form={form}>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="categoryId"
            label="Category ID (UUID)"
            rules={[
              {
                validator: (_, value?: string) => {
                  const trimmed = value?.trim();
                  if (!trimmed || UUID_PATTERN.test(trimmed)) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Category ID must be a valid UUID'));
                },
              },
            ]}
          >
            <Input placeholder="Optional. If set, this takes priority over category name." />
          </Form.Item>
          <Form.Item
            name="category"
            label="Category Name"
            extra="Optional. If both category fields are empty, default category 'General' will be used."
          >
            <Input />
          </Form.Item>
          <Form.Item name="brand" label="Brand">
            <Input />
          </Form.Item>
          <Form.Item name="price" label="Base Price" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} precision={2} />
          </Form.Item>
          <Form.Item
            name="attributesJson"
            label="Attributes (JSON object)"
            rules={[
              {
                validator: (_, value?: string) => {
                  try {
                    parseAttributesJson(value ?? '');
                    return Promise.resolve();
                  } catch (error) {
                    return Promise.reject(error instanceof Error ? error.message : 'Invalid JSON object');
                  }
                },
              },
            ]}
          >
            <Input.TextArea rows={4} placeholder='{"material":"cotton","warranty":"12m"}' />
          </Form.Item>
          <Form.Item name="shortDescription" label="Short Description">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="longDescription" label="Long Description">
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item name="seoTitle" label="SEO Title">
            <Input />
          </Form.Item>
          <Form.Item name="seoDescription" label="SEO Description">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="seoSlug" label="SEO Slug">
            <Input placeholder="auto-generated from name when empty" />
          </Form.Item>
          <Space size="large">
            <Form.Item name="allowBackorder" label="Allow Backorder" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="flashSale" label="Flash Sale" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="trending" label="Trending" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item name="bestSeller" label="Best Seller" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </>
  );
};
