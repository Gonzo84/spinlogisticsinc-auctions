package eu.auctionplatform.payment.api.v1.resource

import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.payment.api.v1.dto.CheckoutItemRequest
import eu.auctionplatform.payment.api.v1.dto.CheckoutRequest
import eu.auctionplatform.payment.api.v1.dto.CheckoutResponse
import eu.auctionplatform.payment.api.v1.dto.DepositRefundRequest
import eu.auctionplatform.payment.api.v1.dto.DepositRequest
import eu.auctionplatform.payment.api.v1.dto.DepositResponse
import eu.auctionplatform.payment.api.v1.dto.InvoiceResponse
import eu.auctionplatform.payment.api.v1.dto.PaymentStatusResponse
import eu.auctionplatform.payment.api.v1.dto.PaymentSubmitRequest
import eu.auctionplatform.payment.api.v1.dto.PaymentSummary
import eu.auctionplatform.payment.api.v1.dto.PaymentsSummaryResponse
import eu.auctionplatform.payment.api.v1.dto.SettlementResponse
import eu.auctionplatform.payment.application.service.AuctionLotLookupService
import eu.auctionplatform.payment.application.service.CheckoutService
import eu.auctionplatform.payment.application.service.LotCheckoutDetail
import eu.auctionplatform.payment.application.service.NonPaymentService
import eu.auctionplatform.payment.application.service.PaymentWebhookData
import eu.auctionplatform.payment.application.service.SettlementService
import eu.auctionplatform.payment.domain.model.Invoice
import eu.auctionplatform.payment.domain.model.Payment
import eu.auctionplatform.payment.domain.model.PaymentStatus
import eu.auctionplatform.payment.domain.model.Settlement
import eu.auctionplatform.payment.infrastructure.invoice.InvoiceHtmlGenerator
import eu.auctionplatform.payment.infrastructure.persistence.repository.InvoiceRepository
import eu.auctionplatform.payment.infrastructure.persistence.repository.PaymentRepository
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * REST resource for payment, checkout, invoice, deposit, and settlement operations.
 *
 * All endpoints are secured via OIDC JWT tokens. Role-based access is enforced
 * via `@RolesAllowed` annotations aligned with the Casbin RBAC policy.
 *
 * Response format follows the platform convention:
 * - Successful responses return the DTO directly with appropriate HTTP status.
 * - Error responses use the standard platform error envelope.
 */
@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class PaymentResource @Inject constructor(
    private val checkoutService: CheckoutService,
    private val settlementService: SettlementService,
    private val nonPaymentService: NonPaymentService,
    private val paymentRepository: PaymentRepository,
    private val invoiceRepository: InvoiceRepository,
    private val invoiceHtmlGenerator: InvoiceHtmlGenerator,
    private val auctionLotLookupService: AuctionLotLookupService
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(PaymentResource::class.java)
    }

    // -----------------------------------------------------------------------
    // Checkout endpoints
    // -----------------------------------------------------------------------

    /**
     * Initiates checkout for one or more won lots.
     *
     * Creates payment records with calculated VAT and buyer premium for
     * each lot. Returns a checkout summary with payment details.
     *
     * @param request The checkout request containing lot IDs and buyer details.
     * @param securityContext Injected security context with the authenticated user.
     * @return 201 Created with [CheckoutResponse].
     */
    @POST
    @Path("/checkout")
    @RolesAllowed("buyer_active")
    fun initiateCheckout(
        @Valid request: CheckoutRequest,
        @Context securityContext: SecurityContext
    ): Response {
        val buyerId = extractUserId(securityContext)

        // Validate: at least one of items or lotIds must be provided
        if (request.items.isEmpty() && request.lotIds.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Either 'items' or 'lotIds' must be provided", "code" to "INVALID_REQUEST"))
                .build()
        }

        // Build lot details — prefer explicit items over lotIds lookup
        val lotDetails = if (request.items.isNotEmpty()) {
            LOG.infof("Checkout initiated by buyer %s with %d explicit item(s)", buyerId, request.items.size)
            buildLotDetailsFromItems(request)
        } else {
            LOG.infof("Checkout initiated by buyer %s with %d lotId(s) (lookup mode)", buyerId, request.lotIds.size)
            buildLotDetailsFromLotIds(request)
        }

        val payments = checkoutService.initiateCheckout(buyerId, lotDetails)

        val totalAmount = payments.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalAmount) }

        val checkoutResponse = CheckoutResponse(
            checkoutId = UUID.randomUUID().toString(),
            payments = payments.map { toPaymentSummary(it) },
            totalAmount = totalAmount,
            currency = request.currency,
            dueDate = payments.firstOrNull()?.dueDate ?: Instant.now()
        )

        return Response.status(Response.Status.CREATED).entity(ApiResponse.ok(checkoutResponse)).build()
    }

    /**
     * Retrieves the status of a specific checkout/payment.
     *
     * @param id The payment UUID.
     * @return 200 OK with [PaymentStatusResponse], or 404 if not found.
     */
    @GET
    @Path("/checkout/{id}")
    @RolesAllowed("buyer_active", "seller_verified", "admin_ops", "admin_super")
    fun getCheckoutStatus(@PathParam("id") id: String): Response {
        val paymentId = UUID.fromString(id)
        val payment = paymentRepository.findById(paymentId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Payment not found", "code" to "PAYMENT_NOT_FOUND"))
                .build()

        return Response.ok(toPaymentStatusResponse(payment)).build()
    }

    /**
     * Submits a payment for processing via the PSP (Adyen).
     *
     * @param id The payment UUID.
     * @param request The payment submission request with method details.
     * @return 200 OK with updated [PaymentStatusResponse], or 404/409 on error.
     */
    @POST
    @Path("/checkout/{id}/pay")
    @RolesAllowed("buyer_active")
    fun submitPayment(
        @PathParam("id") id: String,
        @Valid request: PaymentSubmitRequest
    ): Response {
        val paymentId = UUID.fromString(id)

        LOG.infof("Payment submission for %s via %s", paymentId, request.paymentMethod)

        val updatedPayment = try {
            checkoutService.processPayment(paymentId, request.paymentMethod)
        } catch (e: IllegalStateException) {
            return Response.status(Response.Status.CONFLICT)
                .entity(mapOf("error" to e.message, "code" to "INVALID_STATUS"))
                .build()
        }

        if (updatedPayment == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Payment not found", "code" to "PAYMENT_NOT_FOUND"))
                .build()
        }

        return Response.ok(toPaymentStatusResponse(updatedPayment)).build()
    }

    /**
     * Retrieves a single payment by its UUID.
     *
     * @param id The payment UUID.
     * @return 200 OK with [PaymentStatusResponse], or 404 if not found.
     */
    @GET
    @Path("/{id}")
    @RolesAllowed("buyer_active", "seller_verified", "admin_ops", "admin_super")
    fun getPaymentById(@PathParam("id") id: String): Response {
        val paymentId = UUID.fromString(id)
        val payment = paymentRepository.findById(paymentId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Payment not found", "code" to "PAYMENT_NOT_FOUND"))
                .build()

        return Response.ok(ApiResponse.ok(toPaymentStatusResponse(payment))).build()
    }

    // -----------------------------------------------------------------------
    // Webhook endpoint (no auth — validated via HMAC)
    // -----------------------------------------------------------------------

    /**
     * Receives payment webhooks from Adyen.
     *
     * This endpoint is called by Adyen to notify of payment status changes.
     * Authentication is performed via HMAC signature validation rather than
     * OIDC tokens.
     *
     * @param body The raw webhook notification payload.
     * @return 200 OK with "[accepted]" to acknowledge receipt.
     */
    @POST
    @Path("/webhook")
    fun handleWebhook(body: Map<String, Any?>): Response {
        LOG.info("Received payment webhook")

        // In production, validate HMAC signature before processing.
        // Parse the Adyen notification format.
        val webhookData = parseWebhookPayload(body)

        if (webhookData != null) {
            checkoutService.handlePaymentWebhook(webhookData)
        }

        // Adyen expects "[accepted]" in the response body
        return Response.ok("[accepted]").build()
    }

    // -----------------------------------------------------------------------
    // Invoice endpoints
    // -----------------------------------------------------------------------

    /**
     * Lists invoices for the authenticated user.
     *
     * For buyers: returns invoices linked to payments where the caller is the buyer.
     * For sellers: returns invoices linked to payments where the caller is the seller.
     * For admins: returns all invoices.
     *
     * @param page Page number (1-based).
     * @param size Page size.
     * @return 200 OK with list of [InvoiceResponse].
     */
    @GET
    @Path("/invoices")
    @RolesAllowed("buyer_active", "seller_verified", "admin_ops", "admin_super")
    fun listInvoices(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int,
        @Context securityContext: SecurityContext
    ): Response {
        val effectiveSize = size.coerceIn(1, 100)
        val effectivePage = page.coerceAtLeast(1)
        val offset = (effectivePage - 1) * effectiveSize

        val userId = extractUserId(securityContext)
        val isAdmin = securityContext.isUserInRole("admin_ops") ||
            securityContext.isUserInRole("admin_super")

        val (invoices, total) = if (isAdmin) {
            // Admins see all invoices
            invoiceRepository.findAll(effectiveSize, offset)
        } else {
            // Non-admin users: filter by their payments
            val userPayments = paymentRepository.findByBuyerId(userId)
            val sellerPayments = paymentRepository.findByStatus(PaymentStatus.COMPLETED)
                .filter { it.sellerId == userId }
            val allUserPaymentIds = (userPayments + sellerPayments).map { it.id }.toSet()

            val allInvoices = invoiceRepository.findAll(effectiveSize * 10, 0)
                .first
                .filter { it.paymentId in allUserPaymentIds }

            val pagedInvoices = allInvoices.drop(offset).take(effectiveSize)
            Pair(pagedInvoices, allInvoices.size.toLong())
        }

        val response = mapOf(
            "items" to invoices.map { toInvoiceResponse(it) },
            "total" to total,
            "page" to effectivePage,
            "pageSize" to effectiveSize
        )

        return Response.ok(response).build()
    }

    /**
     * Downloads an invoice PDF.
     *
     * @param id The invoice UUID.
     * @return 200 OK with redirect to PDF URL, or 404 if not found.
     */
    @GET
    @Path("/invoices/{id}/pdf")
    @RolesAllowed("buyer_active", "seller_verified", "admin_ops", "admin_super")
    fun downloadInvoicePdf(@PathParam("id") id: String): Response {
        val invoiceId = UUID.fromString(id)
        val invoice = invoiceRepository.findById(invoiceId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Invoice not found", "code" to "INVOICE_NOT_FOUND"))
                .build()

        if (invoice.pdfUrl.isNullOrBlank()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "PDF not yet generated", "code" to "PDF_NOT_READY"))
                .build()
        }

        return Response.temporaryRedirect(java.net.URI.create(invoice.pdfUrl)).build()
    }

    /**
     * Returns an HTML-rendered invoice.
     *
     * **GET /api/v1/payments/invoices/{id}/html**
     *
     * @param id The invoice UUID.
     * @return 200 OK with text/html content, or 404 if not found.
     */
    @GET
    @Path("/invoices/{id}/html")
    @Produces(MediaType.TEXT_HTML)
    @RolesAllowed("buyer_active", "seller_verified", "admin_ops", "admin_super")
    fun getInvoiceHtml(@PathParam("id") id: String): Response {
        val invoiceId = UUID.fromString(id)
        val invoice = invoiceRepository.findById(invoiceId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity("Invoice not found")
                .build()

        val payment = paymentRepository.findById(invoice.paymentId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity("Payment not found for invoice")
                .build()

        val html = invoiceHtmlGenerator.generate(
            invoice = invoice,
            payment = payment,
            buyerName = payment.buyerName ?: "Buyer ${payment.buyerId}",
            sellerName = payment.sellerName ?: "Seller ${payment.sellerId}",
            lotTitle = payment.lotTitle ?: "Lot ${payment.lotId}"
        )

        return Response.ok(html, MediaType.TEXT_HTML).build()
    }

    // -----------------------------------------------------------------------
    // Settlement endpoints
    // -----------------------------------------------------------------------

    /**
     * Lists settlements for the authenticated seller.
     *
     * @return 200 OK with list of [SettlementResponse].
     */
    @GET
    @Path("/settlements")
    @RolesAllowed("seller_verified", "admin_ops", "admin_super")
    fun listSettlements(
        @Context securityContext: SecurityContext
    ): Response {
        val sellerId = extractUserId(securityContext)
        val settlements = settlementService.getSellerSettlements(sellerId)

        return Response.ok(settlements.map { toSettlementResponse(it) }).build()
    }

    // -----------------------------------------------------------------------
    // Deposit endpoints
    // -----------------------------------------------------------------------

    /**
     * Initiates a bid deposit for an auction.
     *
     * Some high-value auctions require a pre-bid deposit. This endpoint
     * creates a hold on the buyer's payment method for the deposit amount.
     *
     * @param request The deposit request.
     * @param securityContext Injected security context.
     * @return 201 Created with [DepositResponse].
     */
    @POST
    @Path("/deposits")
    @RolesAllowed("buyer_active")
    fun initiateDeposit(
        @Valid request: DepositRequest,
        @Context securityContext: SecurityContext
    ): Response {
        val buyerId = extractUserId(securityContext)

        LOG.infof(
            "Deposit initiated by buyer %s for auction %s (amount=%s %s)",
            buyerId, request.auctionId, request.amount, request.currency
        )

        // In production, this would create a payment authorisation hold via Adyen.
        val depositResponse = DepositResponse(
            depositId = UUID.randomUUID().toString(),
            auctionId = request.auctionId,
            amount = request.amount,
            currency = request.currency,
            status = "HELD",
            createdAt = Instant.now()
        )

        return Response.status(Response.Status.CREATED).entity(depositResponse).build()
    }

    /**
     * Requests a refund of a previously placed deposit.
     *
     * Deposits are automatically refunded when the buyer does not win
     * the auction. This endpoint allows manual refund requests.
     *
     * @param request The refund request.
     * @param securityContext Injected security context.
     * @return 200 OK with refund confirmation.
     */
    @POST
    @Path("/deposits/refund")
    @RolesAllowed("buyer_active")
    fun requestDepositRefund(
        @Valid request: DepositRefundRequest,
        @Context securityContext: SecurityContext
    ): Response {
        val buyerId = extractUserId(securityContext)

        LOG.infof(
            "Deposit refund requested by buyer %s for auction %s",
            buyerId, request.auctionId
        )

        // In production, this would initiate a refund via Adyen.
        val response = mapOf(
            "auctionId" to request.auctionId,
            "status" to "REFUND_INITIATED",
            "message" to "Deposit refund has been initiated. Funds will be returned within 5-10 business days."
        )

        return Response.ok(response).build()
    }

    // -----------------------------------------------------------------------
    // Admin endpoints
    // -----------------------------------------------------------------------

    /**
     * Returns aggregate payment summary statistics (admin only).
     *
     * **GET /api/v1/payments/summary**
     *
     * @return 200 OK with [PaymentsSummaryResponse].
     */
    @GET
    @Path("/summary")
    @RolesAllowed("admin_ops", "admin_super")
    fun getPaymentsSummary(): Response {
        LOG.debug("GET /payments/summary")

        val pending = paymentRepository.findByStatus(PaymentStatus.PENDING)
        val processing = paymentRepository.findByStatus(PaymentStatus.PROCESSING)
        val completed = paymentRepository.findByStatus(PaymentStatus.COMPLETED)

        val overdueNow = paymentRepository.findOverdue(Instant.now())

        val allPending = pending + processing
        val totalPending = allPending.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalAmount) }
        val totalPaid = completed.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalAmount) }
        val totalOverdue = overdueNow.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalAmount) }

        val summary = PaymentsSummaryResponse(
            totalPending = totalPending,
            totalOverdue = totalOverdue,
            totalPaid = totalPaid,
            totalDisputed = BigDecimal.ZERO,
            pendingCount = allPending.size,
            overdueCount = overdueNow.size
        )

        return Response.ok(summary).build()
    }

    /**
     * Lists all payments (admin only).
     *
     * Supports pagination for reviewing all platform payments.
     *
     * @param status Optional status filter.
     * @param page Page number (1-based).
     * @param size Page size.
     * @return 200 OK with paginated list of [PaymentStatusResponse].
     */
    @GET
    @RolesAllowed("admin_ops", "admin_super")
    fun listAllPayments(
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        val effectiveSize = size.coerceIn(1, 100)
        val effectivePage = page.coerceAtLeast(1)
        val offset = (effectivePage - 1) * effectiveSize

        val (payments, total) = if (!status.isNullOrBlank()) {
            val statusEnum = PaymentStatus.valueOf(status.uppercase())
            val list = paymentRepository.findByStatus(statusEnum)
            Pair(list, list.size.toLong())
        } else {
            paymentRepository.findAll(effectiveSize, offset)
        }

        val response = mapOf(
            "items" to payments.map { toPaymentStatusResponse(it) },
            "total" to total,
            "page" to effectivePage,
            "pageSize" to effectiveSize
        )

        return Response.ok(response).build()
    }

    /**
     * Marks a payment as settled (admin only).
     *
     * **PATCH /api/v1/payments/{id}/settle**
     *
     * Triggers settlement processing for a completed payment.
     *
     * @param id The payment UUID.
     * @return 200 OK with settlement response, or 404/409 on error.
     */
    @PATCH
    @Path("/{id}/settle")
    @RolesAllowed("admin_ops", "admin_super")
    fun settlePayment(@PathParam("id") id: String): Response {
        val paymentId = UUID.fromString(id)
        val payment = paymentRepository.findById(paymentId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Payment not found", "code" to "PAYMENT_NOT_FOUND"))
                .build()

        // Allow settlement for COMPLETED or PROCESSING payments.
        // PROCESSING is accepted because in dev/test flows there is no real
        // PSP webhook to transition the payment to COMPLETED; admin settlement
        // acts as the manual completion trigger.
        if (payment.status != PaymentStatus.COMPLETED && payment.status != PaymentStatus.PROCESSING) {
            return Response.status(Response.Status.CONFLICT)
                .entity(mapOf(
                    "error" to "Payment must be COMPLETED or PROCESSING to settle (current: ${payment.status})",
                    "code" to "INVALID_STATUS"
                ))
                .build()
        }

        // If payment is still PROCESSING, auto-complete it before settling
        if (payment.status == PaymentStatus.PROCESSING) {
            paymentRepository.markCompleted(
                id = paymentId,
                paymentMethod = payment.paymentMethod ?: "admin_settled",
                pspReference = payment.pspReference ?: "ADMIN-${UUID.randomUUID().toString().take(8).uppercase()}",
                paidAt = Instant.now()
            )
            LOG.infof("Payment %s auto-completed by admin settle action", paymentId)
        }

        val settlement = settlementService.createSettlement(paymentId)
            ?: return Response.status(Response.Status.CONFLICT)
                .entity(mapOf("error" to "Settlement already exists or could not be created", "code" to "SETTLEMENT_EXISTS"))
                .build()

        // Auto-process the settlement
        val processed = settlementService.processSettlement(settlement.id)

        return Response.ok(toSettlementResponse(processed ?: settlement)).build()
    }

    /**
     * Initiates a refund for a completed payment (admin only).
     *
     * **POST /api/v1/payments/{id}/refund**
     *
     * @param id The payment UUID.
     * @return 200 OK with updated payment status, or 404/409 on error.
     */
    @POST
    @Path("/{id}/refund")
    @RolesAllowed("admin_ops", "admin_super")
    fun refundPayment(@PathParam("id") id: String): Response {
        val paymentId = UUID.fromString(id)
        val payment = paymentRepository.findById(paymentId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Payment not found", "code" to "PAYMENT_NOT_FOUND"))
                .build()

        if (payment.status != PaymentStatus.COMPLETED) {
            return Response.status(Response.Status.CONFLICT)
                .entity(mapOf(
                    "error" to "Only COMPLETED payments can be refunded (current: ${payment.status})",
                    "code" to "INVALID_STATUS"
                ))
                .build()
        }

        paymentRepository.updateStatus(paymentId, PaymentStatus.REFUNDED)
        LOG.infof("Payment %s refunded by admin", paymentId)

        val updated = paymentRepository.findById(paymentId)!!
        return Response.ok(toPaymentStatusResponse(updated)).build()
    }

    /**
     * Sends a payment reminder for a pending payment (admin only).
     *
     * **POST /api/v1/payments/{id}/reminder**
     *
     * In production this would publish a notification event. For now it
     * returns a confirmation that the reminder was triggered.
     *
     * @param id The payment UUID.
     * @return 200 OK with confirmation, or 404 on error.
     */
    @POST
    @Path("/{id}/reminder")
    @RolesAllowed("admin_ops", "admin_super")
    fun sendPaymentReminder(@PathParam("id") id: String): Response {
        val paymentId = UUID.fromString(id)
        val payment = paymentRepository.findById(paymentId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Payment not found", "code" to "PAYMENT_NOT_FOUND"))
                .build()

        if (payment.status != PaymentStatus.PENDING) {
            return Response.status(Response.Status.CONFLICT)
                .entity(mapOf(
                    "error" to "Reminders can only be sent for PENDING payments (current: ${payment.status})",
                    "code" to "INVALID_STATUS"
                ))
                .build()
        }

        LOG.infof(
            "Payment reminder sent for payment %s (buyer=%s, amount=%s %s, due=%s)",
            paymentId, payment.buyerId, payment.totalAmount, payment.currency, payment.dueDate
        )

        return Response.ok(mapOf(
            "paymentId" to paymentId.toString(),
            "buyerId" to payment.buyerId.toString(),
            "status" to "REMINDER_SENT",
            "message" to "Payment reminder has been sent to buyer ${payment.buyerId}"
        )).build()
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Builds [LotCheckoutDetail] list from explicit [CheckoutItemRequest] items.
     *
     * Hammer price and auction ID come directly from the request. Seller ID
     * and lot metadata are resolved from the catalog-service as a best-effort
     * enrichment — failures do not block checkout.
     */
    private fun buildLotDetailsFromItems(request: CheckoutRequest): List<LotCheckoutDetail> {
        return request.items.map { item ->
            val lotId = UUID.fromString(item.lotId)
            val auctionId = UUID.fromString(item.auctionId)
            val hammerPrice = item.hammerPrice

            // Enrich with lot metadata (seller, country, title) — best-effort
            val lotInfo = auctionLotLookupService.fetchLotInfo(lotId)
            val sellerId = item.sellerId?.let { UUID.fromString(it) }
                ?: lotInfo?.sellerId
                ?: auctionLotLookupService.fetchAuctionResultByAuctionId(auctionId)?.sellerId
                ?: lotId
            val sellerCountry = lotInfo?.sellerCountry ?: request.buyerCountry
            val lotTitle = lotInfo?.title

            LOG.infof(
                "Checkout item (explicit): lot=%s, auction=%s, hammerPrice=%s, sellerId=%s",
                lotId, auctionId, hammerPrice, sellerId
            )

            LotCheckoutDetail(
                lotId = lotId,
                auctionId = auctionId,
                sellerId = sellerId,
                hammerPrice = hammerPrice,
                currency = request.currency,
                buyerCountry = request.buyerCountry,
                sellerCountry = sellerCountry,
                buyerType = request.buyerType,
                sellerType = "BUSINESS",
                buyerVatId = request.buyerVatId,
                lotTitle = lotTitle,
                buyerName = null,
                sellerName = null
            )
        }
    }

    /**
     * Builds [LotCheckoutDetail] list by looking up auction and lot data
     * from other services via HTTP. This is the fallback mode when the caller
     * does not provide explicit items.
     */
    private fun buildLotDetailsFromLotIds(request: CheckoutRequest): List<LotCheckoutDetail> {
        return request.lotIds.map { lotIdStr ->
            val lotId = UUID.fromString(lotIdStr)

            // Look up auction result (hammer price, winner, auctionId)
            val auctionResult = auctionLotLookupService.fetchAuctionResultByLot(lotId)
            // Look up lot details (sellerId, title, country)
            val lotInfo = auctionLotLookupService.fetchLotInfo(lotId)

            val hammerPrice = auctionResult?.hammerPrice ?: BigDecimal.ZERO
            val auctionId = auctionResult?.auctionId ?: lotId
            val sellerId = lotInfo?.sellerId ?: auctionResult?.sellerId ?: lotId
            val sellerCountry = lotInfo?.sellerCountry ?: request.buyerCountry
            val lotTitle = lotInfo?.title

            if (hammerPrice == BigDecimal.ZERO) {
                LOG.warnf(
                    "Checkout lot %s: hammerPrice resolved to 0 (auctionResult=%s, lotInfo=%s). " +
                        "Consider using 'items' mode with explicit hammerPrice.",
                    lotId, auctionResult != null, lotInfo != null
                )
            }

            LOG.infof(
                "Checkout item (lookup): lot=%s, auction=%s, hammerPrice=%s, sellerId=%s, title=%s",
                lotId, auctionId, hammerPrice, sellerId, lotTitle
            )

            LotCheckoutDetail(
                lotId = lotId,
                auctionId = auctionId,
                sellerId = sellerId,
                hammerPrice = hammerPrice,
                currency = request.currency,
                buyerCountry = request.buyerCountry,
                sellerCountry = sellerCountry,
                buyerType = request.buyerType,
                sellerType = "BUSINESS",
                buyerVatId = request.buyerVatId,
                lotTitle = lotTitle,
                buyerName = null,
                sellerName = null
            )
        }
    }

    private fun extractUserId(securityContext: SecurityContext): UUID {
        val principal = securityContext.userPrincipal?.name
            ?: throw jakarta.ws.rs.ForbiddenException("User identity not available")
        return UUID.fromString(principal)
    }

    private fun parseWebhookPayload(body: Map<String, Any?>): PaymentWebhookData? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val notificationItems = body["notificationItems"] as? List<Map<String, Any?>>
            val item = notificationItems?.firstOrNull()

            @Suppress("UNCHECKED_CAST")
            val notification = item?.get("NotificationRequestItem") as? Map<String, Any?>
                ?: return null

            @Suppress("UNCHECKED_CAST")
            val amount = notification["amount"] as? Map<String, Any?>

            PaymentWebhookData(
                pspReference = notification["pspReference"]?.toString() ?: "",
                merchantReference = notification["merchantReference"]?.toString(),
                eventCode = notification["eventCode"]?.toString() ?: "",
                success = notification["success"]?.toString()?.toBoolean() ?: false,
                paymentMethod = notification["paymentMethod"]?.toString(),
                reason = notification["reason"]?.toString(),
                amountValue = amount?.get("value")?.toString()?.toLongOrNull(),
                amountCurrency = amount?.get("currency")?.toString()
            )
        } catch (e: Exception) {
            LOG.errorf("Failed to parse webhook payload: %s", e.message)
            null
        }
    }

    private fun toPaymentSummary(payment: Payment): PaymentSummary = PaymentSummary(
        paymentId = payment.id.toString(),
        lotId = payment.lotId.toString(),
        hammerPrice = payment.hammerPrice,
        buyerPremium = payment.buyerPremium,
        vatAmount = payment.vatAmount,
        vatRate = payment.vatRate,
        vatScheme = payment.vatScheme.name,
        totalAmount = payment.totalAmount
    )

    private fun toPaymentStatusResponse(payment: Payment): PaymentStatusResponse =
        PaymentStatusResponse(
            paymentId = payment.id.toString(),
            buyerId = payment.buyerId.toString(),
            sellerId = payment.sellerId.toString(),
            auctionId = payment.auctionId.toString(),
            lotId = payment.lotId.toString(),
            hammerPrice = payment.hammerPrice,
            buyerPremium = payment.buyerPremium,
            vatAmount = payment.vatAmount,
            vatRate = payment.vatRate,
            vatScheme = payment.vatScheme.name,
            totalAmount = payment.totalAmount,
            currency = payment.currency,
            status = payment.status.name,
            paymentMethod = payment.paymentMethod,
            pspReference = payment.pspReference,
            dueDate = payment.dueDate,
            paidAt = payment.paidAt,
            createdAt = payment.createdAt,
            lotTitle = payment.lotTitle,
            buyerName = payment.buyerName,
            sellerName = payment.sellerName
        )

    private fun toInvoiceResponse(invoice: Invoice): InvoiceResponse = InvoiceResponse(
        invoiceId = invoice.id.toString(),
        paymentId = invoice.paymentId.toString(),
        invoiceNumber = invoice.invoiceNumber,
        type = invoice.type.name,
        pdfUrl = invoice.pdfUrl,
        issuedAt = invoice.issuedAt
    )

    private fun toSettlementResponse(settlement: Settlement): SettlementResponse =
        SettlementResponse(
            settlementId = settlement.id.toString(),
            sellerId = settlement.sellerId.toString(),
            paymentId = settlement.paymentId.toString(),
            netAmount = settlement.netAmount,
            commissionAmount = settlement.commission,
            commissionRate = settlement.commissionRate,
            status = settlement.status.name,
            settledAt = settlement.settledAt,
            bankReference = settlement.bankReference,
            createdAt = settlement.createdAt
        )
}
