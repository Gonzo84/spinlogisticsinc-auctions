# SPC Auctions — Product Roadmap

## The Platform Today & What Comes Next

The core SPC Auctions auction platform is **fully built and functional**. Buyers can search, bid in real-time, and check out. Sellers can list items, track sales, and receive settlements. Admins can manage the entire operation. The auction engine, payment flow, search, notifications, and compliance tools all work end-to-end.

What remains before a full commercial launch is connecting the platform to **real-world external services** — a live payment processor, identity verification providers, and production email delivery — and completing the regulatory compliance layer for EU requirements.

This roadmap is organized by business priority, not technical complexity.

---

## Phase 1 — Required for Commercial Launch

These items must be completed before the platform can process real transactions and operate commercially in the EU.

---

### Live Payment Processing

**Today:** The payment flow is fully built — checkout, VAT calculation, buyer premium, settlement, and invoice generation all work. However, payments are currently processed in test/simulation mode. No real money changes hands.

**What's needed:** Connect the platform to **Adyen** (a leading European payment processor) to accept real payments.

**What this enables:**
- Accept credit/debit cards, iDEAL (Netherlands), SEPA direct debit, Bancontact (Belgium), and other European payment methods
- 3D Secure authentication for every transaction — required by EU law (PSD2) for online payments
- Automatic refund processing through the payment provider
- Settlement timing aligned with seller agreements (e.g., payout 1-2 business days after payment confirmation)
- Full PCI-DSS compliance — cardholder data is handled by Adyen, never stored on the platform

**Why it matters:** Without this, the platform cannot generate revenue. This is the single most important item on the roadmap.

---

### Identity Verification (KYC)

**Today:** The platform tracks user verification status but does not actually verify anyone's identity. Users can register and bid without proving who they are.

**What's needed:** Integrate a **KYC (Know Your Customer) provider** such as Onfido, Jumio, or IDnow to verify user identities before they can participate in auctions.

**What this enables:**
- Users upload a photo ID (passport, national ID, driver's license) and the system verifies it automatically
- Liveness detection prevents identity fraud (confirms the person is real, not a photo of a photo)
- Address verification through utility bills or bank statements
- Risk scoring flags high-risk users for manual review
- Bidding is blocked until identity is verified — protecting sellers and the platform from fraud

**Why it matters:** EU regulations require platforms handling financial transactions to verify the identity of their users. Without KYC, the platform is legally exposed and vulnerable to fraudulent accounts.

---

### Anti-Money Laundering (AML) Screening

**Today:** The platform has an AML screening interface, but it currently returns simulated results rather than checking against real sanctions databases.

**What's needed:** Connect to an **AML screening provider** such as ComplyAdvantage, Refinitiv, or LexisNexis to perform real background checks.

**What this enables:**
- Automatic screening against international sanctions lists (UN, EU, OFAC)
- Politically Exposed Person (PEP) checks — identifies high-risk individuals in government or public positions
- Adverse media screening — flags users mentioned in negative news coverage
- Suspicious transaction monitoring — detects unusual bidding or payment patterns
- Automated suspicious activity reporting to the relevant Financial Intelligence Unit (FIU)

**Why it matters:** The EU Anti-Money Laundering Directive (AMLD6) requires platforms facilitating high-value transactions to screen users and report suspicious activity. Non-compliance carries significant fines and potential criminal liability for platform operators.

---

### Production Email Delivery

**Today:** All email notifications (outbid alerts, auction results, payment confirmations) are fully built and working, but they are captured in a test inbox during development rather than delivered to real users.

**What's needed:** Connect to a **production email service** such as AWS SES, SendGrid, or Mailgun.

**What this enables:**
- Real email delivery to buyers, sellers, and administrators
- Professional branded email templates matching the SPC Auctions identity
- Delivery tracking — know if emails were received, opened, or bounced
- Unsubscribe management — required by GDPR and CAN-SPAM regulations
- Rate limiting to prevent spam classification

**Why it matters:** Email is the primary communication channel for time-sensitive auction events (outbid notifications, auction closing, payment due). Without real email delivery, users miss critical updates and the platform feels unresponsive.

---

### GDPR Compliance Completion

**Today:** The platform supports GDPR data export and erasure requests — users can submit them and administrators can manage them. However, the actual data collection and cross-service deletion is not yet fully automated.

**What's needed:** Complete the automated data handling pipeline across all platform services.

**What this enables:**
- When a user requests their data (Article 20 — right to data portability), the platform automatically collects all their information from every service and delivers it as an encrypted download
- When a user requests data deletion (Article 17 — right to be forgotten), the platform automatically removes their data across all services with confirmation
- 30-day deadline enforcement with automatic alerts to administrators if a request approaches the legal deadline
- Privacy policy and cookie consent integration on all portals
- Full audit trail of all data handling for regulatory inspection

**Why it matters:** GDPR fines can reach **4% of annual global turnover** or EUR 20 million, whichever is higher. Partial compliance is not enough — the regulation requires complete, timely, and verifiable data handling.

---

### Internal Security Hardening

**Today:** Every user-facing endpoint is secured with authentication and role-based access control. However, the internal communication between platform services does not yet use authenticated connections.

**What's needed:** Add secure authentication to all internal service-to-service communication.

**What this enables:**
- Every internal request is verified and auditable
- If one service is compromised, it cannot impersonate another
- Full audit trail of all internal operations
- Meets security requirements for financial service platforms

**Why it matters:** For a platform handling financial transactions, internal security is not optional. Enterprise clients and auditors will require proof that internal communications are secured.

---

### Automated Deployment Pipeline

**Today:** The complete deployment pipeline is built and tested — automated building, testing, security scanning, and container packaging. The final step (deploying to production servers) is configured but not yet activated.

**What's needed:** Activate the production deployment stage and connect it to the hosting infrastructure.

**What this enables:**
- Code changes are automatically tested, scanned for security vulnerabilities, and deployed to a staging environment for verification
- Production deployments happen with one click (or automatically on approval)
- If something goes wrong, the platform can roll back to the previous version automatically
- No manual server configuration or file copying — everything is automated and repeatable

**Why it matters:** Manual deployments are slow, error-prone, and risky. An automated pipeline means faster updates, fewer mistakes, and the ability to respond quickly to issues.

---

## Phase 2 — First 3 Months After Launch

These items improve the user experience and complete regulatory requirements. The platform operates without them, but they should be added shortly after launch.

---

### Mobile Push Notifications

**Today:** Users receive email notifications for auction events. Push notifications to mobile devices are not yet active.

**What this adds:** Real-time push alerts on phones and tablets — "You've been outbid!", "Auction closing in 5 minutes", "Payment received". Tapping a notification takes the user directly to the relevant auction or payment.

**Why it matters:** Auctions are time-sensitive. Mobile push notifications drive engagement and reduce the number of missed bids, increasing platform revenue.

---

### Digital Services Act (DSA) Completion

**Today:** Content reporting is in place — users can report inappropriate listings. The moderation workflow and transparency reporting need completion.

**What this adds:** Full DSA compliance including automated moderation workflows, user notifications of decisions, appeal mechanisms, and complete transparency reports as required by EU Regulation 2022/2065.

**Why it matters:** The DSA applies to all online platforms operating in the EU. Non-compliance can result in fines up to **6% of global annual turnover**.

---

### Enhanced Analytics & Reporting

**Today:** Basic analytics dashboards show revenue trends, bid volumes, and user growth. Some metrics need refinement.

**What this adds:** Real-time dashboards (not just daily snapshots), custom report building, data export to CSV/Excel/PDF, seller performance benchmarking, and conversion funnel analytics.

**Why it matters:** Platform operators need comprehensive data to make business decisions — which categories perform best, where to focus marketing, which sellers drive the most revenue.

---

### Advanced User Management

**Today:** Administrators can manage individual users. Bulk operations and detailed activity tracking are limited.

**What this adds:** Search and filter across all users, bulk approve/suspend actions, detailed activity timelines per user, login history, and optional two-factor authentication for high-security accounts.

**Why it matters:** As the user base grows, administrators need efficient tools to manage thousands of accounts without handling them one by one.

---

### Advanced Access Control

**Today:** Role-based access control works for all standard operations. Fine-grained permissions need refinement.

**What this adds:** Granular permission management, field-level access control for sensitive data (e.g., financial details visible only to authorized staff), and an admin interface for managing permissions without code changes.

**Why it matters:** Enterprise clients often require detailed access control policies — different admin team members should see different data based on their role and responsibilities.

---

## Phase 3 — Growth Features (3-6 Months)

These features expand the platform's capabilities and market reach.

---

### Multi-Currency Support

**Today:** All transactions are in EUR.

**What this adds:** Support for GBP, CHF, USD, PLN, CZK, SEK, and other currencies. Automatic exchange rate conversion, multi-currency settlements, and localized price display.

**Why it matters:** Opens the platform to the UK, Switzerland, and non-eurozone EU markets — significantly expanding the addressable market.

---

### Mobile App

**Today:** The platform is fully responsive and works on mobile browsers.

**What this adds:** A dedicated mobile app (iOS and Android) with push notification deep linking, biometric login (fingerprint/face), offline lot browsing, and camera integration for brokers doing on-site lot intake.

**Why it matters:** A native app delivers faster performance, better push notification handling, and presence in app stores for discoverability.

---

### Additional Auction Formats

**Today:** The platform runs English (ascending price) auctions — the most common format in B2B equipment sales.

**What this adds:**
- **Dutch auctions** — price starts high and drops until someone bids (common for perishable goods and bulk inventory)
- **Sealed-bid auctions** — buyers submit one confidential bid; highest wins (used for real estate and high-value unique items)
- **Buy-now-or-bid** — fixed price option alongside the auction (like eBay's Buy It Now)
- **Package auctions** — group multiple lots into a single auction (common for factory liquidations)
- **Wave closing** — groups of auctions close in sequence to manage buyer attention

**Why it matters:** Different asset types suit different auction formats. Offering multiple formats attracts more sellers and enables the platform to handle a wider range of commercial situations.

---

### Automated Seller Onboarding

**Today:** New sellers are approved manually by administrators.

**What this adds:** Self-service onboarding with automatic VAT number validation, company registry verification, tiered seller levels (starter, verified, premium), and trust badges based on performance history.

**Why it matters:** Manual approval creates a bottleneck. Automated onboarding lets the platform scale without proportionally increasing admin workload.

---

### Verified Sustainability Reporting

**Today:** CO2 avoidance is calculated using standard emission factors per equipment category.

**What this adds:** Third-party verified CO2 certificates (ISO 14040/14044), condition-adjusted calculations, and integration with environmental data sources. Buyers and sellers receive verifiable sustainability credentials.

**Why it matters:** Moves sustainability from a marketing claim to a verifiable, certified credential — increasingly important for corporate ESG reporting and procurement policies.

---

## Phase 4 — Future Vision (6-12 Months)

Long-term enhancements that transform SPC Auctions from a single platform into an ecosystem.

---

### AI-Powered Features

- **Smart lot descriptions** — upload photos and AI generates professional listing descriptions automatically
- **Price prediction** — historical data analysis suggests optimal starting prices and reserve amounts
- **Fraud detection** — machine learning identifies suspicious bidding patterns before they cause harm
- **Personalized recommendations** — buyers see lots tailored to their interests and bidding history
- **Support chatbot** — automated first-line support for common buyer and seller questions

### White-Label Platform

- Offer SPC Auctions as a **white-label solution** — auction companies can run their own branded platform powered by SPC Auctions technology
- Custom branding (logo, colors, domain), configurable auction rules and fee structures
- Isolated data per tenant — each client's data is completely separate
- Self-service tenant management portal
- **Revenue model:** monthly SaaS subscription + transaction percentage

### Logistics Integration

- **Shipping cost calculator** — estimated delivery cost based on item dimensions, weight, and distance
- **Carrier integration** — direct booking with DHL, UPS, DB Schenker for heavy equipment transport
- **Pickup scheduling** — coordinate collection dates for large machinery
- **Shipment tracking** — real-time tracking numbers visible to buyer and seller
- **Insurance** — optional transit insurance calculated per lot

### Financial Services

- **Buyer financing** — payment plans for high-value purchases
- **Escrow service** — secure holding of funds for high-value or cross-border transactions
- **Automated VAT reporting** — per-country VAT summaries for filing
- **Accounting integration** — direct export to DATEV (Germany), Xero, or other accounting systems

### Marketplace Extensions

- **Buyer/seller messaging** — in-platform communication for questions about lots
- **Seller storefronts** — dedicated pages showcasing each seller's inventory and reputation
- **Condition reports** — third-party inspection integration for independent item assessment
- **Market price comparison** — benchmark lot prices against market data
- **Professional buyer tools** — bulk bidding, saved searches with alerts, portfolio management
- **API access** — let third parties build integrations via documented webhook subscriptions

---

## Summary

| Phase | Timeline | Focus | Status |
|-------|----------|-------|--------|
| **Core Platform** | Completed | Auctions, bidding, payments, search, notifications, compliance | Done |
| **Phase 1** | Before launch | Live payments, identity verification, AML, email, GDPR, security | Planned |
| **Phase 2** | First 3 months | Push notifications, DSA, analytics, user management | Vision |
| **Phase 3** | 3-6 months | Multi-currency, mobile app, auction formats, seller onboarding, CO2 | Vision |
| **Phase 4** | 6-12 months | AI, white-label, logistics, finance, marketplace | Vision |

**The core auction platform is complete.** The path to commercial launch requires connecting to real external services (payment processor, identity provider, email delivery) and completing EU regulatory compliance. The platform architecture is designed to support all planned features without requiring a rewrite — each phase builds on the existing foundation.
