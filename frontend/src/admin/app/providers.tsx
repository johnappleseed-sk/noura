import { App as AntApp, ConfigProvider, theme as antTheme } from 'antd';
import { type PropsWithChildren, useEffect, useMemo } from 'react';
import { Provider } from 'react-redux';
import { useAppSelector } from '@/app/hooks';
import { store } from '@/app/store';

/**
 * Renders the ThemeProvider component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
const ThemeProvider = ({ children }: PropsWithChildren): JSX.Element => {
  const mode = useAppSelector((state) => state.theme.mode);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', mode === 'dark');
  }, [mode]);

  const config = useMemo(
    () => ({
      algorithm: mode === 'dark' ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
      token: {
        colorPrimary: mode === 'dark' ? '#64a7ff' : '#1565c0',
        colorInfo: mode === 'dark' ? '#64a7ff' : '#1565c0',
        colorSuccess: mode === 'dark' ? '#49c585' : '#0f9d58',
        colorWarning: mode === 'dark' ? '#f6bd4f' : '#c27803',
        colorError: mode === 'dark' ? '#ff7a7a' : '#d93025',
        colorBgBase: mode === 'dark' ? '#0f172a' : '#f4f7fb',
        colorBgContainer: mode === 'dark' ? '#111a2e' : '#ffffff',
        colorBgElevated: mode === 'dark' ? '#15223a' : '#ffffff',
        colorBorder: mode === 'dark' ? '#27344c' : '#d9e2ec',
        colorTextBase: mode === 'dark' ? '#e5edf8' : '#0f172a',
        borderRadius: 14,
        borderRadiusLG: 18,
        fontFamily: '"Manrope", "Plus Jakarta Sans", "IBM Plex Sans", "Segoe UI", sans-serif',
      },
      components: {
        Layout: {
          headerBg: 'transparent',
          bodyBg: 'transparent',
          siderBg: mode === 'dark' ? '#111a2e' : '#f9fbff',
        },
        Menu: {
          itemHeight: 44,
          itemBorderRadius: 10,
          itemColor: mode === 'dark' ? '#c9d7ec' : '#24324a',
          itemSelectedColor: mode === 'dark' ? '#87bcff' : '#0d47a1',
          itemSelectedBg: mode === 'dark' ? 'rgba(100, 167, 255, 0.18)' : '#dceafe',
          itemHoverBg: mode === 'dark' ? 'rgba(100, 167, 255, 0.12)' : '#edf5ff',
        },
        Card: {
          borderRadiusLG: 18,
          paddingLG: 20,
          headerFontSize: 16,
        },
        Table: {
          borderColor: mode === 'dark' ? '#2a3a56' : '#dde6f1',
          headerBg: mode === 'dark' ? '#19263f' : '#f7faff',
          rowHoverBg: mode === 'dark' ? '#182742' : '#f1f6fd',
          headerColor: mode === 'dark' ? '#cddaf0' : '#334155',
        },
        Input: {
          borderRadius: 12,
          controlHeight: 42,
        },
        InputNumber: {
          borderRadius: 12,
          controlHeight: 42,
        },
        Select: {
          borderRadius: 12,
          controlHeight: 42,
        },
        Button: {
          borderRadius: 12,
          controlHeight: 40,
          fontWeight: 600,
        },
        Modal: {
          borderRadiusLG: 18,
        },
        Drawer: {
          colorBgElevated: mode === 'dark' ? '#15223a' : '#ffffff',
        },
      },
    }),
    [mode],
  );

  return (
    <ConfigProvider theme={config}>
      <AntApp>{children}</AntApp>
    </ConfigProvider>
  );
};

/**
 * Renders the AppProviders component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const AppProviders = ({ children }: PropsWithChildren): JSX.Element => (
  <Provider store={store}>
    <ThemeProvider>{children}</ThemeProvider>
  </Provider>
);
