package eu.auctionplatform.user.infrastructure.persistence.entity

import eu.auctionplatform.user.domain.model.Company
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity mapped to the `app.companies` table.
 *
 * Stores the business registration details for users with
 * [eu.auctionplatform.user.domain.model.AccountType.BUSINESS] accounts.
 */
@Entity
@Table(name = "companies", schema = "app")
class CompanyEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "company_name", nullable = false)
    var companyName: String = "",

    @Column(name = "registration_no")
    var registrationNo: String = "",

    @Column(name = "vat_id")
    var vatId: String = "",

    @Column(name = "country", nullable = false)
    var country: String = "",

    @Column(name = "address")
    var address: String = "",

    @Column(name = "city")
    var city: String = "",

    @Column(name = "postal_code")
    var postalCode: String = "",

    @Column(name = "verified", nullable = false)
    var verified: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
) {

    /** Converts this entity to the domain model. */
    fun toDomain(): Company = Company(
        id = id,
        userId = userId,
        companyName = companyName,
        registrationNo = registrationNo,
        vatId = vatId,
        country = country,
        address = address,
        city = city,
        postalCode = postalCode,
        verified = verified
    )

    companion object {

        /** Creates an entity from the domain model. */
        fun fromDomain(company: Company): CompanyEntity = CompanyEntity(
            id = company.id,
            userId = company.userId,
            companyName = company.companyName,
            registrationNo = company.registrationNo,
            vatId = company.vatId,
            country = company.country,
            address = company.address,
            city = company.city,
            postalCode = company.postalCode,
            verified = company.verified
        )
    }
}
