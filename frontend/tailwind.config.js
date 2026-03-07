/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#e0f7ff',
          100: '#b9ecff',
          200: '#8bdeff',
          300: '#57cfff',
          400: '#22bae8',
          500: '#009dbe',
          600: '#007d9a',
          700: '#005f75',
          800: '#00485a',
          900: '#002f3c',
        },
      },
      boxShadow: {
        card: '0 10px 30px -12px rgb(0 0 0 / 0.18)',
      },
    },
  },
  plugins: [],
}
