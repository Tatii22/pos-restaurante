import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        pos: {
          bg: "#f4f5f7",
          card: "#ffffff",
          border: "#e5e7eb",
          text: "#1f2937",
          muted: "#6b7280",
          accent: "#16a34a",
          accentSoft: "#dcfce7"
        }
      },
      boxShadow: {
        pos: "0 10px 24px rgba(16, 24, 40, 0.08)"
      }
    }
  },
  plugins: []
} satisfies Config;
