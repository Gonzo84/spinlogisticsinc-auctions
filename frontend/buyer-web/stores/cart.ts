import { defineStore } from 'pinia'
import type { CartLot, CartTotals } from '~/types/cart'

export interface CartState {
  selectedLots: CartLot[]
  checkoutInProgress: boolean
}

export const useCartStore = defineStore('cart', {
  state: (): CartState => ({
    selectedLots: [],
    checkoutInProgress: false,
  }),

  getters: {
    lotCount: (state): number => {
      return state.selectedLots.length
    },

    totals: (state): CartTotals => {
      const subtotal = state.selectedLots.reduce((sum, lot) => sum + lot.winningBid, 0)
      const buyersPremium = state.selectedLots.reduce((sum, lot) => sum + lot.buyersPremium, 0)
      const vat = state.selectedLots.reduce((sum, lot) => sum + lot.vatAmount, 0)
      const total = subtotal + buyersPremium + vat
      return { subtotal, buyersPremium, vat, total }
    },

    isLotSelected: (state) => (lotId: string): boolean => {
      return state.selectedLots.some((lot) => lot.id === lotId)
    },

    isEmpty: (state): boolean => {
      return state.selectedLots.length === 0
    },
  },

  actions: {
    addLot(lot: CartLot) {
      if (!this.selectedLots.some((l) => l.id === lot.id)) {
        this.selectedLots = [...this.selectedLots, lot]
      }
    },

    removeLot(lotId: string) {
      this.selectedLots = this.selectedLots.filter((l) => l.id !== lotId)
    },

    toggleLot(lot: CartLot) {
      if (this.isLotSelected(lot.id)) {
        this.removeLot(lot.id)
      } else {
        this.addLot(lot)
      }
    },

    selectAll(lots: CartLot[]) {
      this.selectedLots = [...lots]
    },

    clearAll() {
      this.selectedLots = []
    },

    setCheckoutInProgress(value: boolean) {
      this.checkoutInProgress = value
    },
  },
})
