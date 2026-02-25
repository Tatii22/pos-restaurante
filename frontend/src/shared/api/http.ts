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
    if (error.response?.data) {
      return Promise.reject(error.response.data);
    }
    return Promise.reject(error);
  }
);
