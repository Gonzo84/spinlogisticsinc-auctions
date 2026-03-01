import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { CartLot, CartTotals } from '~/types/cart'

export const useCartStore = defineStore('cart', () => {
  const selectedLots = ref<CartLot[]>([])
  const checkoutInProgress = ref(false)

  const lotCount = computed((): number => {
    return selectedLots.value.length
  })

  const totals = computed((): CartTotals => {
    const subtotal = selectedLots.value.reduce((sum, lot) => sum + lot.winningBid, 0)
    const buyersPremium = selectedLots.value.reduce((sum, lot) => sum + lot.buyersPremium, 0)
    const vat = selectedLots.value.reduce((sum, lot) => sum + lot.vatAmount, 0)
    const total = subtotal + buyersPremium + vat
    return { subtotal, buyersPremium, vat, total }
  })

  function isLotSelected(lotId: string): boolean {
    return selectedLots.value.some((lot) => lot.id === lotId)
  }

  const isEmpty = computed((): boolean => {
    return selectedLots.value.length === 0
  })

  function addLot(lot: CartLot) {
    if (!selectedLots.value.some((l) => l.id === lot.id)) {
      selectedLots.value = [...selectedLots.value, lot]
    }
  }

  function removeLot(lotId: string) {
    selectedLots.value = selectedLots.value.filter((l) => l.id !== lotId)
  }

  function toggleLot(lot: CartLot) {
    if (isLotSelected(lot.id)) {
      removeLot(lot.id)
    } else {
      addLot(lot)
    }
  }

  function selectAll(lots: CartLot[]) {
    selectedLots.value = [...lots]
  }

  function clearAll() {
    selectedLots.value = []
  }

  function setCheckoutInProgress(value: boolean) {
    checkoutInProgress.value = value
  }

  return {
    selectedLots,
    checkoutInProgress,
    lotCount,
    totals,
    isLotSelected,
    isEmpty,
    addLot,
    removeLot,
    toggleLot,
    selectAll,
    clearAll,
    setCheckoutInProgress,
  }
})
