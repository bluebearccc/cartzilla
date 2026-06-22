/** @type {import('tailwindcss').Config} */
// Tokens mirror docs/FRONTEND-STITCH-PROMPTS.md "Prompt 0" and the Stitch output.
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        page: '#F8FAFC',
        card: '#FFFFFF',
        border: '#E2E8F0',
        divider: '#EEF2F6',
        ink: { DEFAULT: '#0F172A', secondary: '#475569', muted: '#94A3B8' },
        brand: { DEFAULT: '#4F46E5', hover: '#4338CA', tint: '#EEF2FF' },
        success: { DEFAULT: '#16A34A', tint: '#DCFCE7' },
        warning: { DEFAULT: '#F59E0B', tint: '#FEF3C7' },
        danger: { DEFAULT: '#DC2626', tint: '#FEE2E2' },
        info: { DEFAULT: '#0EA5E9', tint: '#E0F2FE' },
      },
      borderRadius: { DEFAULT: '10px', lg: '16px', xl: '16px', full: '9999px' },
      fontFamily: {
        headline: ['Manrope', 'sans-serif'],
        body: ['Inter', 'sans-serif'],
        sans: ['Inter', 'sans-serif'],
      },
      boxShadow: {
        card: '0 1px 2px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.04)',
      },
      fontSize: {
        h1: ['32px', { lineHeight: '40px', fontWeight: '700' }],
        h2: ['24px', { lineHeight: '32px', fontWeight: '700' }],
        h3: ['18px', { lineHeight: '28px', fontWeight: '600' }],
      },
    },
  },
  plugins: [],
};
