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
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem("pos_username");
      localStorage.removeItem("pos_role");
      localStorage.removeItem("pos_turno_actual");
      window.location.href = "/login";
      return Promise.reject(error);
    }

    if (error.response?.status === 400) {
      console.error("[API 400 ERROR] Status:", error.response.status);
      console.error("[API 400 ERROR] Data:", error.response.data);
    }
    
    if (error.response?.data) {
      return Promise.reject(error.response.data);
    }
    return Promise.reject(error);
  }
);
