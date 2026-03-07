import { Button, Card, Result } from 'antd';
import { useNavigate } from 'react-router-dom';

/**
 * Renders the NotFoundPage component.
 *
 * @returns The rendered component tree.
 */
export const NotFoundPage = (): JSX.Element => {
  const navigate = useNavigate();
  return (
    <Card className="admin-section-card">
      <Result
        status="404"
        title="404"
        subTitle="The requested route does not exist."
        extra={
          <Button type="primary" onClick={() => navigate('/dashboard')}>
            Back to Dashboard
          </Button>
        }
      />
    </Card>
  );
};
