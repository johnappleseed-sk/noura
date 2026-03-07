import { AppProviders } from '@/app/providers';
import { AppRouter } from '@/app/router';
import '@/styles/admin.css';

/**
 * Renders the App component.
 *
 * @returns The rendered component tree.
 */
export const App = (): JSX.Element => (
  <div className="noura-admin-app">
    <AppProviders>
      <AppRouter />
    </AppProviders>
  </div>
);
