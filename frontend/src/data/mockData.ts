import type { CategoryGroup, Product, PromoPanel } from '../types';

export const headerLinks = [
  { id: 'taobao-home', label: 'Taobao homepage', path: '/' },
  { id: 'spring-sale', label: '3·8 is a good start / Grab more', path: '/search?q=3.8' },
];

export const categoryGroups: CategoryGroup[] = [
  {
    id: 'computer-accessories',
    label: 'Computer / Accessories / Home appliances / Mobile phone',
    keywords: ['computer', 'accessories', 'home appliances', 'mobile phone'],
  },
  {
    id: 'industrial-business',
    label: 'Industrial products / Business',
    keywords: ['industrial', 'business'],
  },
  {
    id: 'furniture-home',
    label: 'Furniture / Home decoration',
    keywords: ['furniture', 'home decoration'],
  },
  {
    id: 'women-men-clothing',
    label: "Women's clothing / Men's clothes",
    keywords: ["women's clothing", "men's clothes"],
  },
  {
    id: 'women-men-shoes',
    label: "Women's shoes / Men's shoes",
    keywords: ["women's shoes", "men's shoes"],
  },
  {
    id: 'car-jewelry',
    label: 'Car / Jewelry / Wenwan / Bicycle',
    keywords: ['car', 'jewelry', 'wenwan', 'bicycle'],
  },
  {
    id: 'food-fresh',
    label: 'Food / Fresh food / Alcohol',
    keywords: ['food', 'fresh food', 'alcohol'],
  },
  {
    id: 'mother-baby',
    label: "Mother and baby / Children's",
    keywords: ['mother and baby', "children's"],
  },
];

export const trendingTags = [
  'floor brush',
  'smart appliance',
  'wireless charging',
  'fresh coupons',
  'sports sale',
];

export const promoPanels: PromoPanel[] = [
  {
    id: 'tmall-global',
    title: 'Tmall Global',
    subtitle: 'Imported picks and direct shipping from global stores.',
    accent: 'linear-gradient(135deg, #ff8a50, #ff5a45)',
    ctaText: 'Explore now',
  },
  {
    id: 'juhuasuan',
    title: 'Juhuasuan',
    subtitle: 'Group-buy specials with flash inventory updates every hour.',
    accent: 'linear-gradient(135deg, #ffb347, #ff7a00)',
    ctaText: 'Join deals',
  },
  {
    id: 'ten-billion-subsidy',
    title: 'Ten Billion Subsidies',
    subtitle: 'Daily fixed-price flagship drops from top brands.',
    accent: 'linear-gradient(135deg, #ffc766, #ff8b33)',
    priceItems: [
      { id: 'sub-1', name: 'Home theater set', price: 13899 },
      { id: 'sub-2', name: 'Smart oven', price: 7999 },
      { id: 'sub-3', name: 'Running machine', price: 7899 },
      { id: 'sub-4', name: 'Luxury skincare', price: 12800 },
    ],
  },
  {
    id: 'big-brand-trial',
    title: 'Big Brand Trial',
    subtitle: '淘金币购物钱包用 100元抵1元',
    accent: 'linear-gradient(135deg, #ffd974, #ffb400)',
    ctaText: '立即领取',
  },
];

export const flashSaleProducts: Product[] = [
  {
    id: 'flash-1',
    name: 'AirClean Pro Purifier',
    price: 652,
    originalPrice: 899,
    category: 'Home appliances',
    tags: ['flash sale', 'home'],
    imageGradient: 'linear-gradient(135deg, #cfd9df, #e2ebf0)',
    imageLabel: 'Air Purifier',
    badge: 'Limited',
  },
  {
    id: 'flash-2',
    name: 'FrostLine Double-Door Fridge',
    price: 1999,
    originalPrice: 2499,
    category: 'Home appliances',
    tags: ['flash sale', 'fridge'],
    imageGradient: 'linear-gradient(135deg, #d7d2cc, #f1ece6)',
    imageLabel: 'Fridge',
    badge: 'Hot',
  },
  {
    id: 'flash-3',
    name: 'UltraView 49 Gaming Display',
    price: 7699,
    originalPrice: 8399,
    category: 'Computer',
    tags: ['monitor', 'gaming'],
    imageGradient: 'linear-gradient(135deg, #d5def5, #c9ffbf)',
    imageLabel: 'Monitor',
    badge: 'Top pick',
  },
  {
    id: 'flash-4',
    name: 'CNC Aluminum Workstation Kit',
    price: 7699,
    originalPrice: 8599,
    category: 'Industrial products',
    tags: ['industrial', 'kit'],
    imageGradient: 'linear-gradient(135deg, #f6d365, #fda085)',
    imageLabel: 'Workstation',
    badge: 'New',
  },
];

export const gridProducts: Product[] = [
  {
    id: 'grid-1',
    name: '四角气雾透明防摔壳',
    price: 29,
    originalPrice: 59,
    category: 'Mobile phone',
    tags: ['phone case', 'transparent', 'shockproof'],
    imageGradient: 'linear-gradient(135deg, #f3e7e9, #e3eeff)',
    imageLabel: 'Phone Case',
  },
  {
    id: 'grid-2',
    name: '全棉柔巾 宠爱新生肌',
    price: 39,
    originalPrice: 69,
    category: 'Mother and baby',
    tags: ['cotton', 'daily necessities', 'baby'],
    imageGradient: 'linear-gradient(135deg, #f6f9d4, #d6efed)',
    imageLabel: 'Cotton Towel',
  },
  {
    id: 'grid-3',
    name: '直接按压 不用工具',
    price: 16,
    originalPrice: 39,
    category: 'Home decoration',
    tags: ['press install', 'no tools', 'home'],
    imageGradient: 'linear-gradient(135deg, #d4fc79, #96e6a1)',
    imageLabel: 'Quick Fix',
  },
  {
    id: 'grid-4',
    name: 'Electronic Version of Stamps, Jeans Button No-Sew Waist Adjustments',
    price: 19,
    originalPrice: 49,
    category: 'Clothing fashion',
    tags: ['jeans', 'no sew', 'fashion'],
    imageGradient: 'linear-gradient(135deg, #ffecd2, #fcb69f)',
    imageLabel: 'Waist Adjust',
  },
  {
    id: 'grid-5',
    name: 'Portable Outdoor Solar Lamp Kit',
    price: 118,
    originalPrice: 168,
    category: 'Sports and outdoors',
    tags: ['outdoor', 'solar', 'camp'],
    imageGradient: 'linear-gradient(135deg, #fdfbfb, #ebedee)',
    imageLabel: 'Outdoor Lamp',
  },
  {
    id: 'grid-6',
    name: 'Organic Salmon Fillet Family Pack',
    price: 88,
    originalPrice: 120,
    category: 'Fresh food',
    tags: ['fresh food', 'seafood'],
    imageGradient: 'linear-gradient(135deg, #fad0c4, #ffd1ff)',
    imageLabel: 'Fresh Food',
  },
  {
    id: 'grid-7',
    name: 'Smart Sports Bottle with Temp Display',
    price: 79,
    originalPrice: 129,
    category: 'Sports and outdoors',
    tags: ['sports', 'daily necessities'],
    imageGradient: 'linear-gradient(135deg, #84fab0, #8fd3f4)',
    imageLabel: 'Sports Bottle',
  },
  {
    id: 'grid-8',
    name: 'Daily Essentials Travel Organizer 80+ 包',
    price: 45,
    originalPrice: 89,
    category: 'Daily necessities',
    tags: ['organizer', 'travel', '80+ bag'],
    imageGradient: 'linear-gradient(135deg, #fccb90, #d57eeb)',
    imageLabel: 'Organizer',
  },
];

export const guessYouLikeCategories = [
  'Sports and outdoors',
  'Fresh food',
  'Daily necessities',
  'Clothing fashion',
];

export const goldenTipsProduct =
  '户外便携式充电宝防水耐磨不沾沙草加厚材质/便携式手提包';

export const allProducts: Product[] = [...flashSaleProducts, ...gridProducts];
