package eu.auctionplatform.user.infrastructure.persistence.entity

import eu.auctionplatform.user.domain.model.KycRecord
import eu.auctionplatform.user.domain.model.KycStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity mapped to the `app.kyc_records` table.
 *
 * Stores individual KYC verification attempts for a user, including the
 * external provider reference and verification outcome.
 */
@Entity
@Table(name = "kyc_records", schema = "app")
class KycRecordEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "provider", nullable = false, updatable = false)
    var provider: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: KycStatus = KycStatus.PENDING,

    @Column(name = "check_id")
    var checkId: String? = null,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
) {

    /** Converts this entity to the domain model. */
    fun toDomain(): KycRecord = KycRecord(
        id = id,
        userId = userId,
        provider = provider,
        status = status,
        checkId = checkId,
        completedAt = completedAt
    )

    companion object {

        /** Creates an entity from the domain model. */
        fun fromDomain(record: KycRecord): KycRecordEntity = KycRecordEntity(
            id = record.id,
            userId = record.userId,
            provider = record.provider,
            status = record.status,
            checkId = record.checkId,
            completedAt = record.completedAt
        )
    }
}
