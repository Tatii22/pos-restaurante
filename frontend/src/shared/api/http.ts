import axios from "axios";
import { TOKEN_KEY } from "../utils";

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? ""
});

http.interceptors.request.use((config) => {
  const url = config.url ?? "";
  if (url.includes("/api/v1/auth/login") || url.endsWith("/auth/login")) {
    if (config.headers?.Authorization) {
      delete config.headers.Authorization;
    }
    return config;
  }
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    // Log full error for debugging
    if (error.response?.status === 400) {
      console.error("[API 400 ERROR] Status:", error.response.status);
      console.error("[API 400 ERROR] Data:", error.response.data);
      console.error("[API 400 ERROR] Full error:", JSON.stringify(error, Object.getOwnPropertyNames(error), 2));
    }
    
    if (error.response?.data) {
      return Promise.reject(error.response.data);
    }
    return Promise.reject(error);
  }
);
