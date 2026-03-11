import type { Page } from '@playwright/test'

const mockLots = [
  {
    id: '550e8400-e29b-41d4-a716-446655440001',
    title: 'Industrial CNC Machine',
    description: 'High-precision CNC milling machine, 2020 model',
    category: 'Metalworking',
    status: 'PUBLISHED',
    startingPrice: 15000,
    currency: 'EUR',
    locationCity: 'Amsterdam',
    locationCountry: 'NL',
    brand: 'troostwijk',
    images: [],
    createdAt: '2026-01-15T10:00:00Z',
    updatedAt: '2026-01-15T10:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440002',
    title: 'Forklift Toyota 8FBE15',
    description: 'Electric forklift, excellent condition',
    category: 'Logistics',
    status: 'PUBLISHED',
    startingPrice: 8500,
    currency: 'EUR',
    locationCity: 'Berlin',
    locationCountry: 'DE',
    brand: 'surplex',
    images: [],
    createdAt: '2026-01-16T10:00:00Z',
    updatedAt: '2026-01-16T10:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440003',
    title: 'Packaging Line Complete',
    description: 'Automated packaging line with conveyor system',
    category: 'Packaging',
    status: 'PUBLISHED',
    startingPrice: 25000,
    currency: 'EUR',
    locationCity: 'Rotterdam',
    locationCountry: 'NL',
    brand: 'industrial-auctions',
    images: [],
    createdAt: '2026-01-17T10:00:00Z',
    updatedAt: '2026-01-17T10:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440004',
    title: 'Welding Robot ABB IRB',
    description: 'ABB IRB 6700 welding robot with controller',
    category: 'Robotics',
    status: 'PUBLISHED',
    startingPrice: 32000,
    currency: 'EUR',
    locationCity: 'Warsaw',
    locationCountry: 'PL',
    brand: 'troostwijk',
    images: [],
    createdAt: '2026-01-18T10:00:00Z',
    updatedAt: '2026-01-18T10:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440005',
    title: 'Air Compressor Atlas Copco',
    description: 'Screw compressor 75kW, serviced 2025',
    category: 'Compressors',
    status: 'PUBLISHED',
    startingPrice: 12000,
    currency: 'EUR',
    locationCity: 'Milan',
    locationCountry: 'IT',
    brand: 'surplex',
    images: [],
    createdAt: '2026-01-19T10:00:00Z',
    updatedAt: '2026-01-19T10:00:00Z',
  },
]

const mockAuctions = [
  {
    auctionId: '660e8400-e29b-41d4-a716-446655440001',
    lotId: '550e8400-e29b-41d4-a716-446655440001',
    status: 'OPEN',
    startingBid: 15000,
    currentHighBid: 16500,
    bidCount: 3,
    startTime: '2026-03-01T09:00:00Z',
    endTime: '2026-03-15T17:00:00Z',
    currency: 'EUR',
  },
  {
    auctionId: '660e8400-e29b-41d4-a716-446655440002',
    lotId: '550e8400-e29b-41d4-a716-446655440002',
    status: 'OPEN',
    startingBid: 8500,
    currentHighBid: 9200,
    bidCount: 5,
    startTime: '2026-03-01T09:00:00Z',
    endTime: '2026-03-15T17:00:00Z',
    currency: 'EUR',
  },
]

export async function setupApiMocks(page: Page) {
  // Mock catalog-service lot listings
  await page.route('**/api/v1/lots*', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      json: {
        data: {
          items: mockLots,
          total: mockLots.length,
          page: 1,
          pageSize: 20,
        },
      },
    })
  )

  // Mock catalog-service single lot detail
  await page.route('**/api/v1/lots/*', (route) => {
    const url = route.request().url()
    const lotId = url.split('/lots/')[1]?.split('?')[0]
    const lot = mockLots.find((l) => l.id === lotId) || mockLots[0]
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      json: { data: lot },
    })
  })

  // Mock auction-engine auction listings
  await page.route('**/api/v1/auctions*', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      json: {
        data: {
          items: mockAuctions,
          total: mockAuctions.length,
          page: 1,
          pageSize: 20,
        },
      },
    })
  )

  // Mock search-service
  await page.route('**/api/v1/search*', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      json: {
        data: {
          items: mockLots,
          total: mockLots.length,
          page: 1,
          pageSize: 20,
        },
      },
    })
  )

  // Mock categories
  await page.route('**/api/v1/categories*', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      json: {
        data: [
          { id: '1', name: 'Metalworking', slug: 'metalworking' },
          { id: '2', name: 'Logistics', slug: 'logistics' },
          { id: '3', name: 'Packaging', slug: 'packaging' },
          { id: '4', name: 'Robotics', slug: 'robotics' },
          { id: '5', name: 'Compressors', slug: 'compressors' },
        ],
      },
    })
  )

  // Mock notifications (empty for unauthenticated)
  await page.route('**/api/v1/notifications*', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      json: { data: { items: [], total: 0, page: 1, pageSize: 20 } },
    })
  )

  // Mock Keycloak well-known endpoint to prevent OIDC init errors
  await page.route('**/realms/auction-platform/.well-known/**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      json: {
        issuer: 'http://localhost:8180/realms/auction-platform',
        authorization_endpoint: 'http://localhost:8180/realms/auction-platform/protocol/openid-connect/auth',
        token_endpoint: 'http://localhost:8180/realms/auction-platform/protocol/openid-connect/token',
        end_session_endpoint: 'http://localhost:8180/realms/auction-platform/protocol/openid-connect/logout',
        jwks_uri: 'http://localhost:8180/realms/auction-platform/protocol/openid-connect/certs',
        response_types_supported: ['code'],
        grant_types_supported: ['authorization_code', 'refresh_token'],
        subject_types_supported: ['public'],
      },
    })
  )
}
