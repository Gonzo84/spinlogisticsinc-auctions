package eu.auctionplatform.user.domain.model

/**
 * Status of a Know-Your-Customer verification check.
 *
 * - **PENDING** – The KYC check has been initiated but has not yet completed.
 * - **VERIFIED** – The identity provider has confirmed the user's identity.
 * - **REJECTED** – The identity verification failed or was rejected by the provider.
 */
enum class KycStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
