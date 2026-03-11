import { test, expect } from '@playwright/test'
import { setupApiMocks } from './fixtures/api-mocks'

test.describe('Homepage', () => {
  test.beforeEach(async ({ page }) => {
    await setupApiMocks(page)
  })

  test('should load the homepage and display the main heading', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveTitle(/auction/i)
  })

  test('should display navigation bar', async ({ page }) => {
    await page.goto('/')
    const nav = page.locator('nav, header')
    await expect(nav.first()).toBeVisible()
  })

  test('should have a working search link or input', async ({ page }) => {
    await page.goto('/')
    // Look for a search input or a link to the search page
    const searchElement = page.locator(
      'input[type="search"], input[placeholder*="search" i], a[href*="/search"]'
    )
    const count = await searchElement.count()
    expect(count).toBeGreaterThan(0)
  })
})
