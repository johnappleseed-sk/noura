export type RoleType = 'ADMIN' | 'CUSTOMER' | 'B2B';

export interface AuthTokensResponse {
  userId: string;
  email: string;
  fullName: string;
  roles: RoleType[];
  accessToken: string;
  refreshToken: string;
}

export interface DashboardSummary {
  revenue: number;
  ordersCount: number;
  usersCount: number;
  storesCount: number;
  topProducts: string[];
  storePerformance: string[];
}

export interface Product {
  id: string;
  name: string;
  category: string;
  brand: string;
  price: number;
  flashSale: boolean;
  trending: boolean;
  bestSeller: boolean;
  averageRating: number;
  reviewCount: number;
  popularityScore: number;
  shortDescription?: string;
  longDescription?: string;
  seoTitle?: string;
  seoDescription?: string;
  seoSlug?: string;
  attributes?: Record<string, unknown>;
  status?: string;
  active?: boolean;
  allowBackorder?: boolean;
}

export interface ProductRequest {
  name: string;
  description?: string;
  categoryId?: string;
  category?: string;
  brand?: string;
  price: number;
  attributes?: Record<string, unknown>;
  allowBackorder: boolean;
  flashSale: boolean;
  trending: boolean;
  bestSeller: boolean;
  shortDescription?: string;
  longDescription?: string;
  seoTitle?: string;
  seoDescription?: string;
  seoSlug?: string;
  variants: Array<{ color: string; size: string; sku: string }>;
  media: Array<{ mediaType: string; url: string; sortOrder: number }>;
  inventory: Array<{ storeId: string; stock: number; storePrice: number }>;
}

export type OrderStatus =
  | 'CREATED'
  | 'REVIEWED'
  | 'PAYMENT_PENDING'
  | 'PAID'
  | 'PACKED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED';

export type RefundStatus = 'NONE' | 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'COMPLETED';

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  userId: string;
  storeId?: string | null;
  subtotal: number;
  discountAmount: number;
  shippingAmount: number;
  totalAmount: number;
  fulfillmentMethod: 'PICKUP' | 'DELIVERY';
  status: OrderStatus;
  refundStatus: RefundStatus;
  couponCode?: string | null;
  createdAt: string;
  items: OrderItem[];
}

export type NotificationCategory = 'ORDER' | 'SYSTEM' | 'STORE' | 'AI' | 'SECURITY';

export interface NotificationItem {
  id: string;
  targetUserId?: string | null;
  category: NotificationCategory;
  title: string;
  body: string;
  read: boolean;
  createdAt: string;
}

export interface OrderTimelineEvent {
  id: string;
  orderId: string;
  status: OrderStatus;
  refundStatus: RefundStatus;
  actor?: string | null;
  note?: string | null;
  createdAt: string;
}

export type StoreServiceType = 'PICKUP' | 'DELIVERY' | 'CURBSIDE' | 'B2B_DESK';

export interface Store {
  id: string;
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
  distanceKm: number;
  openNow: boolean;
}

export interface StoreRequest {
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

export interface UserProfile {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  roles: RoleType[];
  enabled: boolean;
  preferredStoreId?: string | null;
}

export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface Approval {
  id: string;
  requesterId: string;
  orderId?: string | null;
  amount: number;
  status: ApprovalStatus;
  reviewerNotes?: string;
  createdAt: string;
}
