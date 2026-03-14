/**
 * Branded types to prevent accidental confusion between catalog lot IDs
 * and auction-engine auction IDs at compile time.
 *
 * Usage:
 *   const lotId = asCatalogLotId('some-uuid')
 *   const auctionId = asAuctionId('other-uuid')
 *   // Type error: Argument of type 'AuctionId' is not assignable to 'CatalogLotId'
 */

export type CatalogLotId = string & { readonly __brand: 'CatalogLotId' }
export type AuctionId = string & { readonly __brand: 'AuctionId' }

export function asCatalogLotId(id: string): CatalogLotId {
  return id as CatalogLotId
}

export function asAuctionId(id: string): AuctionId {
  return id as AuctionId
}
