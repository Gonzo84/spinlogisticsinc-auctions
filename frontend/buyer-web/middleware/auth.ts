export default defineNuxtRouteMiddleware(async (to) => {
  const { isAuthenticated, login } = useAuth()

  if (!isAuthenticated.value) {
    await login(window.location.origin + to.fullPath)
    return abortNavigation()
  }
})
