import type { Auction, Bid } from '~/types/auction'

/**
 * Computes the minimum bid increment based on the current bid amount,
 * matching the backend's tiered increment rules in AuctionConstants.
 */
export function computeMinIncrement(currentBid: number): number {
  if (currentBid >= 100_000) return 500
  if (currentBid >= 50_000) return 250
  if (currentBid >= 10_000) return 100
  if (currentBid >= 5_000) return 50
  if (currentBid >= 1_000) return 25
  if (currentBid >= 500) return 10
  if (currentBid >= 100) return 5
  return 1
}

/**
 * Maps backend auction + optional lot catalog data to frontend Auction type.
 * Handles field name differences between auction-engine and catalog-service.
 */
export function mapAuctionResponse(auction: Record<string, unknown>, lot?: Record<string, unknown>): Auction {
  const lotImages = lot?.images as Array<Record<string, unknown>> | undefined
  return {
    id: (auction.auctionId ?? auction.id ?? '') as string,
    title: (lot?.title ?? auction.title ?? `Lot ${((auction.lotId ?? auction.auctionId ?? '') as string).substring(0, 8)}`) as string,
    lotNumber: (auction.lotId ?? lot?.id ?? '') as string,
    description: (lot?.description ?? auction.description ?? '') as string,
    category: (lot?.categoryId ?? auction.category ?? '') as string,
    country: (lot?.locationCountry ?? auction.country ?? '') as string,
    location: lot ? `${lot.locationCity ?? ''}, ${lot.locationCountry ?? ''}`.replace(/^, |, $/, '') : (auction.location ?? '') as string,
    images: lotImages?.map((img: Record<string, unknown>) => ({ url: img.imageUrl as string, thumbnail: img.thumbnailUrl as string })) ?? auction.images as Auction['images'] ?? [],
    currentBid: (auction.currentHighBid ?? auction.currentBid ?? lot?.startingBid ?? auction.startingBid ?? 0) as number,
    startingPrice: (auction.startingBid ?? lot?.startingBid ?? auction.startingPrice ?? 0) as number,
    bidCount: (auction.bidCount ?? 0) as number,
    bidHistory: (auction.bidHistory ?? []) as Bid[],
    endTime: (auction.endTime ?? lot?.auctionEnd ?? lot?.endTime ?? '') as string,
    startTime: (auction.startTime ?? lot?.auctionStart ?? lot?.startTime ?? '') as string,
    status: ((auction.status ?? 'active') as string).toLowerCase() as Auction['status'],
    reserveMet: (auction.reserveMet ?? false) as boolean,
    co2Savings: (lot?.co2AvoidedKg ?? auction.co2Savings) as number | undefined,
    specifications: lot?.specifications
      ? Object.entries(lot.specifications as Record<string, unknown>).map(([key, value]) => ({ key, label: key, value: String(value) }))
      : (auction.specifications ?? []) as Auction['specifications'],
    seller: auction.seller as Auction['seller'],
    depositRequired: (auction.depositRequired ?? false) as boolean,
    depositAmount: auction.depositAmount as number | undefined,
    minIncrement: (auction.minIncrement ?? computeMinIncrement((auction.currentHighBid ?? auction.currentBid ?? auction.startingBid ?? 0) as number)) as number,
    featured: auction.featured as boolean | undefined,
  }
}
