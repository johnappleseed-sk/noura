import { Space, Typography } from 'antd';
import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  extra?: ReactNode;
}

/**
 * Renders the PageHeader component.
 *
 * @param param0 The param0 value.
 * @returns The rendered component tree.
 */
export const PageHeader = ({ title, subtitle, extra }: PageHeaderProps): JSX.Element => (
  <div className="admin-page-header">
    <div>
      <Typography.Title className="admin-page-title" level={2}>
        {title}
      </Typography.Title>
      {subtitle ? <Typography.Paragraph className="admin-page-subtitle">{subtitle}</Typography.Paragraph> : null}
    </div>
    {extra ? <Space>{extra}</Space> : null}
  </div>
);
