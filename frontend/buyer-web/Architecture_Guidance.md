I'll research the best architecture principles and code conventions for Nuxt applications.Here's a comprehensive summary of the best architecture principles and code conventions for Nuxt applications, based on current community consensus:

---

## Architecture Principles

**1. Convention Over Configuration**

Nuxt follows the convention over configuration principle, providing sensible defaults for your project structure. It uses an opinionated directory structure to automate repetitive tasks and allow developers to focus on pushing features. Resist deviating from these conventions unless you have a strong reason to.

**2. Modular Architecture**

A modular architecture breaks an application into self-contained, reusable units called modules that encapsulate specific functionality. Each module can be developed, tested, and maintained independently. For larger apps, this prevents the problem where your components folder has 50+ files, your stores reference each other in complex ways, and a seemingly innocent change to the cart accidentally breaks the product listing page.

**3. Nuxt Layers for Scale (Modular Monolith)**

For medium-to-large projects, Nuxt Layers can be used to build a modular monolith architecture — clean boundaries and enforced separation of concerns, without the complexity of microservices. Layers act as mini-applications stitched together, organized by domain (e.g., `blog/`, `home/`, `admin/`). Layers can be viewed as "mini" applications which are stitched together to create the "full" application.

**4. Recommended Directory Structure**

The standard structure includes: `components/` for reusable Vue components, `composables/` for reusable composable functions, `layouts/` for application layouts, `middleware/` for route middleware, `pages/` for application pages, `plugins/` for Nuxt plugins, `server/` for API routes and server-side logic, `store/` for Pinia stores, and `utils/` for utility functions.

**5. Hybrid Rendering Strategy**

Nuxt lets you decide rendering per route — keep marketing pages pre-rendered for speed, serve dashboards with live data through SSR, or flip the whole app to SPA mode. Choose SSR, SSG, or client-side rendering based on each route's needs rather than a one-size-fits-all approach.

**6. Full-Stack with Nitro**

Drop a file into `/server/api/hello.ts` and the Nitro engine exposes it at `/api/hello`. Use the `server/` directory for API endpoints, middleware, and server utilities, keeping backend logic colocated with the frontend.

---

## Code Conventions

**Naming Conventions**

- Use lowercase with dashes for directories (e.g., `components/auth-wizard`). Use PascalCase for component names (e.g., `AuthWizard.vue`). Use camelCase for composables (e.g., `useAuthState.ts`).
- Composables should be named as `use<MyComposable>`. Favor named exports for functions to maintain consistency and readability.
- Use descriptive variable names with auxiliary verbs (e.g., `isLoading`, `hasError`).

**Composition API & TypeScript**

- Use `<script setup>` syntax for concise component definitions. Leverage `ref`, `reactive`, and `computed` for reactive state management. Use `provide/inject` for dependency injection when appropriate. Implement custom composables for reusable logic.
- Use TypeScript for all code. Use composition API and declarative programming patterns; avoid the Options API. Avoid enums; use const objects instead.

**Data Fetching**

- Use `useFetch` for standard data fetching in components that benefit from SSR and caching. Use `$fetch` for client-side requests within event handlers. Use `useAsyncData` for complex data fetching logic like combining multiple API calls. Set `lazy: true` to defer non-critical data fetching until after the initial render.

**State Management**

- Use Pinia for state management. Treat state as immutable. Use functions to update the store rather than directly manipulating data. Divide stores into modules based on features or functionalities.

**Component Design**

- Divide your application into small, reusable components. Each component should have a single responsibility, making it easier to test, debug, and reuse.
- Nested component folders become part of the component name, which prevents naming collisions and makes the code self-documenting.

**Composables vs Utils**

- The `composables/` folder is intended for stateful functions — composables in the Vue.js sense, always prefixed with `use`. The `utils/` folder is for stateless utility functions. Keep them separated for long-term maintainability.

**Performance**

- Leverage Nuxt's built-in performance optimizations. Use Suspense for async components. Implement lazy loading for routes and components. Optimize images using WebP format and `<NuxtImage>`. Optimize Web Vitals (LCP, CLS, FID).
- Avoid memory leaks by cleaning up event listeners and timers when components are unmounted. Use virtualization for large lists.

**SEO**

- Implement SEO best practices using Nuxt's `useHead` and `useSeoMeta`.

**Tooling**

- Use ESLint with `@nuxt/eslint` for consistent code style
- Use Husky + lint-staged for pre-commit linting
- Use VueUse for common composables and utility functions.
- Nuxt DevTools for debugging and application insight

---

The overarching philosophy: lean into Nuxt's conventions, keep components small and focused, extract shared logic into composables, use Pinia for state, TypeScript everywhere, and scale with Layers when the project outgrows a flat directory structure.
