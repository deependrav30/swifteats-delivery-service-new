Catalog Cache — Keys, TTLs and Invalidation

- Cache name: catalog.restaurant.menu
- Redis key pattern: catalog.restaurant.menu::<restaurantId>
- TTL: configured by property `catalog.cache.restaurantMenuTtlMinutes` (default: 5 minutes)
- What is cached: the restaurant menu (list of menu items) returned by GET /restaurants/{id}/menu
- Invalidation: any write that replaces the menu (PUT /restaurants/{id}/menu) or creates a restaurant persists to DB and evicts the cache for that restaurant via @CacheEvict.

Notes:
- The cache configuration is defined in `catalog-service/src/main/java/com/swifteats/catalog/config/CacheConfig.java`.
- To change TTL at runtime for tests/local, set environment variable `CATALOG_MENU_TTL_MINUTES` or application property `catalog.cache.restaurantMenuTtlMinutes`.
