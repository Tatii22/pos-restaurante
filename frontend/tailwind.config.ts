import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        pos: {
          bg: "#F4F7F5",
          card: "#FFFFFF",
          border: "#D8EDE4",
          text: "#2E2E2E",
          muted: "#8E918F",
          accent: "#3EB489",
          accentSoft: "#D8EDE4",
          forest: "#2A7B5E",
          mint: "#3EB489"
        }
      },
      boxShadow: {
        pos: "0 10px 24px rgba(16, 24, 40, 0.08)"
      }
    }
  },
  plugins: []
} satisfies Config;
