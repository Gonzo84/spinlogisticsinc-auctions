package eu.auctionplatform.user.infrastructure.persistence.entity

import eu.auctionplatform.user.domain.model.Deposit
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * JPA entity mapped to the `app.deposits` table.
 *
 * Stores security deposit records for users, including payment and
 * refund lifecycle timestamps and the PSP transaction reference.
 */
@Entity
@Table(name = "deposits", schema = "app")
class DepositEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    var amount: BigDecimal = BigDecimal("200.00"),

    @Column(name = "currency", nullable = false)
    var currency: String = "EUR",

    @Column(name = "paid_at")
    var paidAt: Instant? = null,

    @Column(name = "refund_requested_at")
    var refundRequestedAt: Instant? = null,

    @Column(name = "refunded_at")
    var refundedAt: Instant? = null,

    @Column(name = "psp_reference")
    var pspReference: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
) {

    /** Converts this entity to the domain model. */
    fun toDomain(): Deposit = Deposit(
        id = id,
        userId = userId,
        amount = amount,
        currency = currency,
        paidAt = paidAt,
        refundRequestedAt = refundRequestedAt,
        refundedAt = refundedAt,
        pspReference = pspReference
    )

    companion object {

        /** Creates an entity from the domain model. */
        fun fromDomain(deposit: Deposit): DepositEntity = DepositEntity(
            id = deposit.id,
            userId = deposit.userId,
            amount = deposit.amount,
            currency = deposit.currency,
            paidAt = deposit.paidAt,
            refundRequestedAt = deposit.refundRequestedAt,
            refundedAt = deposit.refundedAt,
            pspReference = deposit.pspReference
        )
    }
}
