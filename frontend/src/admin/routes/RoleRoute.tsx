import { Alert } from 'antd';
import { Outlet } from 'react-router-dom';
import { useAppSelector } from '@/app/hooks';
import { selectRoles } from '@/features/auth/authSlice';
import type { RoleType } from '@/types/models';

interface RoleRouteProps {
  allowed: RoleType[];
}

/**
 * Renders the RoleRoute component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const RoleRoute = ({ allowed }: RoleRouteProps): JSX.Element => {
  const roles = useAppSelector(selectRoles);
  const permitted = allowed.some((role) => roles.includes(role));

  if (!permitted) {
    return (
      <Alert
        showIcon
        type="error"
        message="Access denied"
        description="Your role does not have permission to access this route."
      />
    );
  }

  return <Outlet />;
};
