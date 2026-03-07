export interface CategoryGroup {
  id: string;
  label: string;
  keywords: string[];
}

export interface Product {
  id: string;
  name: string;
  price: number;
  originalPrice?: number;
  category: string;
  tags: string[];
  imageGradient: string;
  imageLabel: string;
  badge?: string;
  description?: string;
}

export interface PromoPriceItem {
  id: string;
  name: string;
  price: number;
}

export interface PromoPanel {
  id: string;
  title: string;
  subtitle: string;
  ctaText?: string;
  accent: string;
  priceItems?: PromoPriceItem[];
}

export interface CartItem extends Product {
  quantity: number;
}
