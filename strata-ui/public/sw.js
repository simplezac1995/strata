/**
 * Strata Admin Service Worker
 *
 * Provides:
 * - Static asset caching (Cache-First strategy)
 * - API request caching (Network-First with offline fallback)
 * - Offline page for navigation requests
 * - Background sync for failed requests (optional)
 */

const CACHE_NAME = "strata-admin-v1";
const STATIC_CACHE = "strata-static-v1";
const API_CACHE = "strata-api-v1";

// Static assets to pre-cache
const PRECACHE_URLS = [
  "/",
  "/index.html",
  "/manifest.webmanifest",
  "/favicon.svg",
  "/icons.svg",
  "/offline.html",
];

// API patterns to cache (Network-First)
const API_PATTERNS = ["/api/system/dict", "/api/system/config", "/api/system/menu"];

// Install event: pre-cache static assets
self.addEventListener("install", (event) => {
  console.log("[SW] Installing Service Worker...");
  event.waitUntil(
    caches
      .open(STATIC_CACHE)
      .then((cache) => {
        console.log("[SW] Pre-caching static assets");
        return cache.addAll(PRECACHE_URLS);
      })
      .then(() => self.skipWaiting())
      .catch((error) => {
        console.warn("[SW] Pre-cache failed:", error);
      }),
  );
});

// Activate event: clean up old caches
self.addEventListener("activate", (event) => {
  console.log("[SW] Activating Service Worker...");
  event.waitUntil(
    caches
      .keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames
            .filter((name) => name !== STATIC_CACHE && name !== API_CACHE)
            .map((name) => {
              console.log("[SW] Deleting old cache:", name);
              return caches.delete(name);
            }),
        );
      })
      .then(() => self.clients.claim()),
  );
});

// Fetch event: route requests based on type
self.addEventListener("fetch", (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Only handle same-origin requests
  if (url.origin !== self.location.origin) {
    return;
  }

  // Navigation requests: Network-First with offline fallback
  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request)
        .then((response) => {
          // Cache the page for offline use
          const responseClone = response.clone();
          caches.open(STATIC_CACHE).then((cache) => cache.put(request, responseClone));
          return response;
        })
        .catch(() => {
          return caches.match(request).then((cached) => cached || caches.match("/offline.html"));
        }),
    );
    return;
  }

  // Static assets: Cache-First
  if (isStaticAsset(request)) {
    event.respondWith(
      caches.match(request).then((cached) => {
        if (cached) {
          return cached;
        }
        return fetch(request)
          .then((response) => {
            if (response.ok && response.status === 200) {
              const responseClone = response.clone();
              caches.open(STATIC_CACHE).then((cache) => cache.put(request, responseClone));
            }
            return response;
          })
          .catch(() => cached);
      }),
    );
    return;
  }

  // API requests: Network-First with cache fallback for specific patterns
  if (isApiRequest(request) && shouldCacheApi(url.pathname)) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response.ok && request.method === "GET") {
            const responseClone = response.clone();
            caches.open(API_CACHE).then((cache) => cache.put(request, responseClone));
          }
          return response;
        })
        .catch(() => caches.match(request)),
    );
    return;
  }

  // Other requests: Network-Only (default browser behavior)
});

// Message event: handle messages from the app
self.addEventListener("message", (event) => {
  const { type } = event.data;

  switch (type) {
    case "SKIP_WAITING":
      self.skipWaiting();
      break;
    case "CLEAR_CACHE":
      caches.keys().then((names) => {
        names.forEach((name) => caches.delete(name));
      });
      break;
    case "GET_VERSION":
      event.source.postMessage({ type: "VERSION", version: CACHE_NAME });
      break;
  }
});

// Background sync: retry failed requests (if supported)
self.addEventListener("sync", (event) => {
  if (event.tag === "sync-failed-requests") {
    event.waitUntil(retryFailedRequests());
  }
});

// Push notification (if supported)
self.addEventListener("push", (event) => {
  if (event.data) {
    const data = event.data.json();
    const options = {
      body: data.body,
      icon: "/favicon.svg",
      badge: "/favicon.svg",
      data: data.url || "/",
      actions: data.actions || [],
    };
    event.waitUntil(self.registration.showNotification(data.title, options));
  }
});

// Notification click
self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = event.notification.data || "/";
  event.waitUntil(
    clients.matchAll({ type: "window", includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if (client.url === url && "focus" in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(url);
      }
    }),
  );
});

// === Helper functions ===

/**
 * Check if a request is for a static asset.
 */
function isStaticAsset(request) {
  const url = new URL(request.url);
  const extensions = [
    ".js",
    ".css",
    ".svg",
    ".png",
    ".jpg",
    ".jpeg",
    ".gif",
    ".ico",
    ".woff",
    ".woff2",
    ".ttf",
    ".eot",
  ];
  return (
    extensions.some((ext) => url.pathname.endsWith(ext)) ||
    url.pathname.startsWith("/assets/") ||
    url.pathname.startsWith("/node_modules/")
  );
}

/**
 * Check if a request is an API request.
 */
function isApiRequest(request) {
  const url = new URL(request.url);
  return url.pathname.startsWith("/api/");
}

/**
 * Check if an API endpoint should be cached.
 */
function shouldCacheApi(pathname) {
  return API_PATTERNS.some((pattern) => pathname.startsWith(pattern));
}

/**
 * Retry failed requests from IndexedDB (simplified).
 */
async function retryFailedRequests() {
  // This is a placeholder for a more sophisticated background sync implementation
  // In production, store failed requests in IndexedDB and retry them here
  console.log("[SW] Retrying failed requests...");
}
