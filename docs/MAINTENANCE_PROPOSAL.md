# SPC Auctions EU Auction Platform - Maintenance & Support Proposal

> Internal pricing guide - not for client distribution as-is

---

## Platform Complexity Assessment

Before pricing, understand what the client is getting maintained:

| Factor | Detail                                           | Complexity |
|--------|--------------------------------------------------|------------|
| Backend services | 13 Kotlin/Quarkus microservices                  | High |
| Frontend apps | 4 Vue 3 applications (1 SSR + 3 SPA)             | High |
| Databases | 14 PostgreSQL databases + Redis + Elasticsearch  | High |
| Infrastructure | NATS, Keycloak, MinIO, Prometheus, Grafana, OTel | High |
| Event architecture | 40+ domain events, CQRS, event sourcing          | High |
| Compliance scope | GDPR, AML, DSA, PSD2, EU VAT                     | High |
| Integrations | Payment gateway, email, S3, search, WebSocket    | Medium-High |
| i18n | 8 languages                                      | Medium |
| **Overall** |                                                  | **Enterprise-grade** |

A comparable SaaS platform on the market would charge EUR 50,000-150,000+ just for the initial build. The maintenance should reflect that value.

---

## Lights-On Plan - EUR 500/mo (Hosting Included)

### What "Lights On" Means

"Lights on" maintenance ensures the platform **stays running, stays secure, and stays accessible** without any feature changes or active development. Think of it as the minimum required to keep the doors open — the servers are monitored, backups are running, security patches are applied, and if something breaks, it gets fixed. No new features, no improvements, no redesigns — just keeping what already works, working.

**In plain terms for the client:** "We make sure your platform doesn't go down, doesn't get hacked, and doesn't lose data. If something breaks, we fix it. If you want something new or changed — we'll quote it separately."

### What's Included

**EUR 500/mo covers hosting infrastructure + maintenance:**

| Category | What's Included | Detail |
|----------|----------------|--------|
| **Hosting infrastructure** | Hetzner dedicated server (16-core, 128 GB RAM, 2x 1.92 TB NVMe), S3 object storage, automated backups, SSL certificates, DNS, CDN | All 13 services, 4 frontends, all databases, full infrastructure running 24/7 |
| **Server monitoring** | Weekly Grafana/Prometheus dashboard checks, automated uptime alerts | We know when something is down before the client calls |
| **Security patching** | OS updates, Docker image updates, critical CVE patches (quarterly cycle) | Keeps the platform safe from known vulnerabilities |
| **Database maintenance** | Backup verification, PostgreSQL VACUUM/ANALYZE, disk space monitoring | Prevents silent data corruption and storage exhaustion |
| **SSL/Domain management** | Certificate auto-renewal monitoring, DNS health | No expired certificates, no downtime from DNS issues |
| **Bug fixes** | Up to 2 bug fixes per month for issues in existing functionality | Something that worked before and broke gets fixed |
| **Monthly status report** | Brief report: uptime, incidents, patches applied, recommendations | Client knows the state of their platform |

### What's NOT Included

The lights-on plan explicitly does **not** include:

- New features or functionality
- UI/UX changes or redesigns
- New pages, components, or API endpoints
- Performance optimization or refactoring
- New integrations (payment providers, APIs, etc.)
- New language/translation additions
- Compliance implementation (real KYC, AML screening)
- 24/7 on-call or weekend support
- Training or knowledge transfer

### Cost Breakdown (Internal)

| Item | EUR/mo |
|------|--------|
| Hetzner AX102-U dedicated server | ~99 |
| Hetzner Object Storage (1 TB) | ~5 |
| Hetzner Storage Box backup (1 TB) | ~4 |
| Floating IP | ~4.50 |
| AWS SES email (~10k emails) | ~1 |
| Cloudflare CDN + DNS | 0 |
| Let's Encrypt SSL | 0 |
| Domain (.eu annual, amortized) | ~0.50 |
| **Infrastructure subtotal** | **~114** |
| **Maintenance margin** | **~386** |
| **Total** | **500** |

At ~386 margin, this covers roughly 5-6 hours of actual maintenance work per month — enough for weekly checks, monthly patching, 2 bug fixes, and reporting.

---

## Feature Changes & Additional Development

Any work beyond the lights-on scope (new features, design changes, integrations, improvements) is handled on a **quoted basis**:

### How It Works

1. **Client requests a change** — via email or agreed communication channel
2. **We assess the scope** — analyze what's needed, which services are affected, estimate hours required
3. **We send a written quote** — hours required, hourly rate, expected timeline, and total cost
4. **Client approves before we start** — no work begins without written approval
5. **We implement, test, and deploy** — with proper staging validation
6. **Client receives an itemized invoice** — hours spent, description of work done

**No surprises. No hidden costs. No work without prior agreement.**

### Hourly Rates

| Work Type                                   | Rate | Notes |
|---------------------------------------------|------|-------|
| Bug fixes,Feature development                                   | EUR 75/hr | Billed at actual hours |
| Urgent/weekend work                         | EUR 130/hr | Emergency  |


---

## What Counts as a "Bug Fix" vs "Feature Change"

| Bug Fix (included in plan) | Feature Change (quoted separately) |
|----------------------------|------------------------------------|
| Something that worked before and stopped | Something that never existed |
| API returning wrong data | New API endpoint |
| Broken UI component | New page or component |
| Login/auth issues | New user role or permission |
| Email not sending | New email template type |
| Search returning wrong results | New search filter |
| Payment calculation error | New payment method |
| WebSocket disconnecting | New real-time feature |
| Mobile responsiveness broken | Mobile app development |
| i18n key missing/wrong | New language addition |
| Styling regression after update | Design overhaul or rebrand |

**Rule of thumb:** If it used to work and doesn't anymore, it's a bug fix. If it never existed, it's a feature — and it gets quoted.

---

## Routine Maintenance Schedule (All Plans)

```
Weekly:
  - Check Grafana dashboards for anomalies
  - Review error logs across 13 services
  - Verify backup completion
  - Check disk space and DB growth

Monthly:
  - Apply critical security patches
  - PostgreSQL VACUUM ANALYZE on large tables
  - Review Elasticsearch index health
  - Verify Keycloak token refresh working
  - Send status report to client

Quarterly:
  - Update Docker base images (JRE, Node, nginx)
  - Review and rotate secrets/passwords
  - SSL certificate audit
  - NATS JetStream consumer health check
  - Dependency security scan (OWASP + npm audit)

Annually:
  - Major framework upgrades (quoted separately if >4 hrs)
  - Infrastructure architecture review
  - Disaster recovery drill
  - Security audit coordination
```

---

## Pricing Justification

### Why This is Good Value

| Comparison | Monthly Cost |
|------------|-------------|
| In-house junior DevOps (EU) | EUR 3,000-4,500 (salary + taxes) |
| In-house senior DevOps (EU) | EUR 5,000-8,000 (salary + taxes) |
| Agency retainer (full-stack, EU) | EUR 3,000-10,000 |
| SaaS platform maintenance (enterprise) | EUR 5,000-15,000 |
| Generic managed hosting | EUR 500-2,000 (no app-level support) |
| **Our Lights-On plan** | **EUR 500 (hosting + maintenance)** |

### Risk Without Maintenance

- **Unpatched vulnerability** → data breach → GDPR fine up to 4% of annual turnover
- **Database runs out of space** → platform crashes → lost auctions and revenue
- **SSL certificate expires** → browsers block access → buyers can't bid
- **Keycloak token issue** → nobody can log in → complete platform outage
- **Docker image CVE** → exploitable attack vector → reputational damage

EUR 500/mo is insurance against all of these.

---

## Recommended Starting Point

| Plan | Monthly | What You Get |
|------|---------|-------------|
| **Lights-On** | **EUR 500** | Hosting + monitoring + patching + 2 bug fixes + reporting |
| Feature changes | Quoted per request | Hours estimated, rate communicated, approval required before work starts |

**Annual cost: EUR 6,000** — less than one month of an in-house developer's loaded cost, and the platform stays running, secure, and maintained year-round.
