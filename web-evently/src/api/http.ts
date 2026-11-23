import axios, { type AxiosError } from "axios";
import { useAuthStore } from "../stores/auth";

const baseURL = import.meta.env.VITE_API_BASE_URL || "/api";
export const http = axios.create({ baseURL });

function tryGetAuthStore() {
    try {
        return useAuthStore();
    } catch {
        return null;
    }
}

http.interceptors.request.use((config) => {
    const store = tryGetAuthStore();
    const token =
        store?.accessToken ??
        JSON.parse(localStorage.getItem("auth") || "null")?.accessToken;
    if (token && !config.headers.has("Authorization")) {
        config.headers.set("Authorization", `Bearer ${token}`);
    }
    return config;
});

let refresh: Promise<void> | undefined;

http.interceptors.response.use(
    (r) => r,
    async (error: AxiosError) => {
        const original = error.config as (typeof error.config & { _retry?: boolean });
        if (original && !original.url?.startsWith("/auth/") && error.response?.status === 401 && !original._retry) {
            const store = tryGetAuthStore();
            if (!store?.refreshToken) {
                store?.clearSession();
                return Promise.reject(error);
            }
            try {
                if (original.headers.get("Authorization") === `Bearer ${store.accessToken}`) {
                    refresh ??= store.refreshTokens().finally(() => { refresh = undefined; });
                    await refresh;
                }
            } catch {
                store.clearSession();
                return Promise.reject(error);
            }
            original._retry = true;
            original.headers.set("Authorization", `Bearer ${store.accessToken}`);
            return http(original);
        }

        try {
            const { useToastsStore } = await import("../stores/toasts");
            const toasts = useToastsStore();
            const status = error.response?.status;
            const data = error.response?.data as any;
            const message =
                (Array.isArray(data?.message)
                    ? data.message.join(", ")
                    : data?.message) ||
                data?.error ||
                error.message ||
                "Ocurrió un error";

            toasts.push({
                type: status && status >= 500 ? "error" : "warn",
                text: `${status ?? ""} ${message}`.trim(),
            });
        } catch {}

        return Promise.reject(error);
    },
);
