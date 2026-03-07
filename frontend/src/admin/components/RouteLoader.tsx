import { Spin, Typography } from 'antd';

/**
 * Renders the RouteLoader component.
 *
 * @returns The rendered component tree.
 */
export const RouteLoader = (): JSX.Element => (
  <div className="admin-route-loader" role="status" aria-live="polite">
    <Spin size="large" />
    <Typography.Text type="secondary">Loading workspace...</Typography.Text>
  </div>
);
