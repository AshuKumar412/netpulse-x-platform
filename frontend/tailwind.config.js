/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        netpulse: {
          bg: '#0a0d14',
          card: '#111622',
          border: '#1e2638',
          muted: '#64748b',
          accent: '#0284c7',
          accentHover: '#0369a1',
        },
      },
    },
  },
  plugins: [],
}
