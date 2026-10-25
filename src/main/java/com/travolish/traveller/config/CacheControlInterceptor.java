package com.travolish.traveller.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Attaches HTTP Cache-Control headers based on the request path.
 *
 * Public endpoints (hotel search, rooms):
 *   Cache-Control: public, max-age=300, s-maxage=600, stale-while-revalidate=60
 *   — browsers cache for 5 min; CDN caches for 10 min; serve stale up to 60 s while revalidating.
 *
 * Private / auth-gated endpoints (users, wishlists, notifications, bookings, auth):
 *   Cache-Control: private, no-store
 *   — no browser disk cache, no CDN caching; prevents data leaking to shared-device users.
 */
public class CacheControlInterceptor implements HandlerInterceptor {

    private static final String PUBLIC_CACHE =
            "public, max-age=300, s-maxage=600, stale-while-revalidate=60";

    private static final String PRIVATE_NO_STORE = "private, no-store";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String path = request.getRequestURI();

        if (isPublic(path)) {
            response.setHeader("Cache-Control", PUBLIC_CACHE);
        } else if (isPrivate(path)) {
            response.setHeader("Cache-Control", PRIVATE_NO_STORE);
        }

        return true;
    }

    private boolean isPublic(String path) {
        return path.startsWith("/api/hotels/search")
                || path.startsWith("/api/rooms");
    }

    private boolean isPrivate(String path) {
        return path.startsWith("/api/users")
                || path.startsWith("/api/wishlists")
                || path.startsWith("/api/notifications")
                || path.startsWith("/api/auth")
                || path.startsWith("/api/bookings")
                || path.startsWith("/api/payments");
    }
}
