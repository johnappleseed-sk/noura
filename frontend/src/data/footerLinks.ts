// src/data/footerLinks.ts
export interface FooterLink {
  label: string;          // can be a translation key or plain text
  href: string;
  isExternal?: boolean;
}

export const footerLinks: FooterLink[] = [
  { label: 'Rules for public discussion', href: '/rules' },
  { label: 'Official service', href: 'https://official.service.com', isExternal: true },
  { label: 'Feedback', href: '/feedback' },
] as const;