package eu.auctionplatform.user

import eu.auctionplatform.user.domain.model.AccountType
import eu.auctionplatform.user.domain.model.DepositStatus
import eu.auctionplatform.user.domain.model.User
import eu.auctionplatform.user.domain.model.UserStatus
import java.time.Instant
import java.util.UUID

object TestFixtures {
    val BUYER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val SELLER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val BROKER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
    val ADMIN_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000004")

    fun createUser(
        id: UUID = BUYER_ID,
        keycloakId: String = "kc-${id}",
        email: String = "buyer@test.com",
        firstName: String = "Test",
        lastName: String = "Buyer",
        accountType: AccountType = AccountType.BUSINESS,
        status: UserStatus = UserStatus.ACTIVE,
        depositStatus: DepositStatus = DepositStatus.NONE
    ) = User(
        id = id,
        keycloakId = keycloakId,
        accountType = accountType,
        email = email,
        firstName = firstName,
        lastName = lastName,
        language = "en",
        currency = "EUR",
        status = status,
        depositStatus = depositStatus,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    fun createSeller(
        id: UUID = SELLER_ID,
        email: String = "seller@test.com"
    ) = createUser(
        id = id,
        email = email,
        firstName = "Test",
        lastName = "Seller",
        depositStatus = DepositStatus.ACTIVE
    )

    fun createUserRegistrationRequest(
        email: String = "newuser@test.com",
        firstName: String = "New",
        lastName: String = "User",
        accountType: String = "BUSINESS"
    ) = mapOf(
        "email" to email,
        "firstName" to firstName,
        "lastName" to lastName,
        "accountType" to accountType,
        "language" to "en",
        "currency" to "EUR"
    )
}
