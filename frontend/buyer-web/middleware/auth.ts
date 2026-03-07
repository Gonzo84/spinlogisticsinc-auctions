export default defineNuxtRouteMiddleware(async (to) => {
  // Skip auth check during SSR — window is not available server-side
  if (!import.meta.client) {
    return
  }

  const { isAuthenticated, login } = useAuth()

  if (!isAuthenticated.value) {
    await login(window.location.origin + to.fullPath)
    return abortNavigation()
  }
})
