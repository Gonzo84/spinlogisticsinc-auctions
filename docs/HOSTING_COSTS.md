# SPC Auctions EU Auction Platform - Monthly Hosting Cost Analysis

> Last updated: March 2026 | Prices in EUR (excl. VAT unless noted)

---

## What Needs to Be Hosted

| Component | Count | RAM (per instance) | Notes |
|-----------|-------|--------------------|-------|
| Kotlin/Quarkus microservices | 13 | 512 MB - 1 GB | Java 21, each with own DB |
| Vue frontend apps | 4 | 128-256 MB | 1 Nuxt SSR + 3 nginx SPAs |
| PostgreSQL 16 | 1 instance, 14 DBs | 4-8 GB | Database-per-service pattern |
| Redis 7 | 1 | 1-2 GB | Auction state, bid timers |
| Elasticsearch 8 | 1 | 4-8 GB | Full-text lot search |
| NATS JetStream | 1-3 nodes | 512 MB each | Event messaging |
| Keycloak 24 | 1 | 1-2 GB | OAuth2/OIDC auth |
| MinIO / Object Storage | 1 | 512 MB | Lot images (100 GB - 1 TB) |
| Prometheus + Grafana + OTel | 3 pods | 2-3 GB total | Monitoring stack |
| **Total RAM needed** | | **~28-40 GB** | |
| **Total vCPUs needed** | | **~16-24** | |

---

## Option 1: Hetzner Dedicated Server (Recommended for Launch)

**The most cost-effective approach for an EU B2B platform.**

### A) Single Server - MVP / Early Stage

Everything on one powerful dedicated server with Docker Compose.

| Item | Spec | EUR/mo |
|------|------|--------|
| Dedicated server | AX102-U: 16c/32t Ryzen 9, 128 GB DDR5, 2x 1.92 TB NVMe | 99.00 |
| Object Storage | Hetzner S3-compatible (1 TB storage + 1 TB egress included) | 4.99 |
| Backup | Storage Box BX11 (1 TB, automated pg_dump) | 3.89 |
| Floating IP | 1x IPv4 for DNS | 4.49 |
| Domain (.eu) | Annual amortized | ~0.50 |
| SSL | Let's Encrypt (free) | 0.00 |
| CDN | Cloudflare Free | 0.00 |
| Email (SMTP) | AWS SES (~10k emails/mo) | ~1.00 |
| **Total** | | **~114/mo** |

- Unlimited bandwidth at 1 Gbit/s
- 128 GB RAM (you need ~35 GB - massive headroom)
- Local NVMe RAID 1 for data durability
- Simple Docker Compose deployment (already works)
- Single point of failure (acceptable for early B2B with maintenance windows)

### B) Two Servers - Production Ready

Separate app tier from data tier for I/O isolation and partial redundancy.

| Item | Spec | EUR/mo |
|------|------|--------|
| App server | AX42-U: 8c/16t Ryzen 7, 64 GB DDR5, 2x 512 GB NVMe | 49.00 |
| Data server | AX102-U: 16c/32t Ryzen 9, 128 GB DDR5, 2x 1.92 TB NVMe | 99.00 |
| Load Balancer | LB11 (SSL termination, health checks) | 5.49 |
| Object Storage | Hetzner S3-compatible | 4.99 |
| Backup | Storage Box BX11 | 3.89 |
| Floating IP | 1x IPv4 | 4.49 |
| Private Network | vSwitch (free between servers in same DC) | 0.00 |
| Domain + SSL + CDN + SMTP | Same as above | ~1.50 |
| **Total** | | **~168/mo** |

- App server: 13 services + 4 frontends + Keycloak + monitoring
- Data server: PostgreSQL + Elasticsearch + Redis + NATS
- 10 Gbit/s private network between servers (free)
- PostgreSQL streaming replication as warm standby possible
- Zero-downtime deploys with blue-green on app server

### C) Kubernetes (k3s on Hetzner Cloud VMs)

For teams wanting auto-healing, rolling updates, and HPA. Matches existing Helm charts.

| Item | Spec | Qty | EUR/mo |
|------|------|-----|--------|
| Control plane | CAX11 (2 vCPU ARM, 4 GB) | 3 | 11.37 |
| App workers | CAX31 (8 vCPU ARM, 16 GB) | 2 | 24.98 |
| Data worker | CCX33 (8 dedicated vCPU, 32 GB) | 1 | 46.49 |
| Volumes | PG 200 GB + ES 200 GB + misc 50 GB | 3 | 23.58 |
| Load Balancer | LB11 | 1 | 5.49 |
| Object Storage | Hetzner S3-compatible | 1 | 4.99 |
| Floating IP | 1x IPv4 | 1 | 4.49 |
| Domain + SSL + CDN + SMTP | | | ~1.50 |
| **Total** | | | **~123/mo** |

- Uses ARM instances (Java 21 + Quarkus + ES all support ARM natively)
- Dedicated CPU for data worker (no noisy-neighbor for PostgreSQL/ES)
- 20 TB traffic included per VM (120+ TB total)
- Higher ops complexity (self-managed k3s, no managed K8s from Hetzner)
- Use `kube-hetzner` Terraform module for automated provisioning

---

## Option 2: DigitalOcean (Managed Services, Simple UX)

Good middle ground - some managed services, simpler than hyperscalers.

| Item | Spec | EUR/mo |
|------|------|--------|
| Kubernetes (DOKS) | 3x General Purpose 8 vCPU/16 GB nodes | ~378 |
| Managed PostgreSQL | 2 vCPU, 4 GB RAM | ~60 |
| Managed Redis (Valkey) | 1 GB single node | ~15 |
| Managed OpenSearch | 1 node, 2 GB | ~19 |
| Spaces (S3) | 250 GB storage + 1 TB egress | ~5 |
| Load Balancer | Regional | ~12 |
| Domain + SSL + CDN + SMTP | | ~1.50 |
| **Total** | | **~491/mo** |

With HA PostgreSQL + standby nodes: **~580/mo**

---

## Option 3: AWS (Full Enterprise Stack)

Maximum managed services, highest reliability, highest cost.

| Item | Spec | EUR/mo |
|------|------|--------|
| EKS control plane | Managed Kubernetes | 73 |
| Worker nodes | 3x t3.xlarge (4 vCPU, 16 GB) eu-west-1 | 405 |
| RDS PostgreSQL | db.t4g.medium Multi-AZ (2 vCPU, 4 GB) + 50 GB | 108 |
| ElastiCache Redis | cache.t3.small Multi-AZ (1.37 GB) | 50 |
| OpenSearch | 2x t3.medium.search + 50 GB | 80 |
| S3 | 100 GB storage | 3 |
| ALB | Application Load Balancer | 24 |
| NAT Gateway | 3x AZ (required for private subnets) | 100 |
| CloudFront | 100 GB egress | 0 (free tier) |
| SES | 10k emails | 1 |
| Route 53 | 1 hosted zone | 1 |
| EBS volumes | 3x 50 GB gp3 | 12 |
| Public IPs | 3x worker nodes | 11 |
| **Total** | | **~868/mo** |

With 1-year Reserved Instances (30-40% savings on compute): **~600/mo**

---

## Option 4: Google Cloud (GKE)

Strong Kubernetes experience, competitive pricing with committed use.

| Item | Spec | EUR/mo |
|------|------|--------|
| GKE control plane | 1 zonal cluster (first is free) | 0 |
| Worker nodes | 3x e2-standard-4 (4 vCPU, 16 GB) europe-west1 | 315 |
| Cloud SQL PostgreSQL | 2 vCPU, 4 GB, HA + 50 GB SSD | 161 |
| Memorystore Redis | 1 GB Standard (HA) | 66 |
| Elastic Cloud | 1 node, 2 GB (on GCP) | 30 |
| Cloud Storage | 100 GB Standard | 2 |
| Cloud NAT | 1 gateway | 33 |
| Cloud CDN | 100 GB | 9 |
| Domain + SSL + SMTP | | ~2 |
| **Total** | | **~618/mo** |

With 1-year committed use discounts: **~450/mo**

---

## Option 5: Azure (AKS)

Free control plane tier available, good for Microsoft-aligned teams.

| Item | Spec | EUR/mo |
|------|------|--------|
| AKS control plane | Free tier (no SLA) | 0 |
| Worker nodes | 3x D4as_v5 (4 vCPU, 16 GB) West Europe | 411 |
| Database for PostgreSQL | Flexible, B2s, Zone-redundant HA + 50 GB | 107 |
| Azure Cache for Redis | Standard C1 (1 GB) | 50 |
| Elasticsearch (Elastic Cloud) | 1 node on Azure | 30 |
| Blob Storage | 100 GB Hot | 2 |
| Standard LB | | 18 |
| NAT Gateway | | 33 |
| Domain + SSL + CDN + SMTP | | ~3 |
| **Total** | | **~654/mo** |

---

## Ancillary Services (Included in Totals Above)

These costs apply regardless of cloud provider:

| Service | Option | EUR/mo |
|---------|--------|--------|
| **Domain (.eu)** | Annual registration | ~0.50 |
| **DNS** | Cloudflare (free) | 0 |
| **SSL/TLS** | Let's Encrypt + cert-manager | 0 |
| **CDN** | Cloudflare Free (unlimited bandwidth) | 0 |
| **Email/SMTP** | AWS SES (10k emails) | ~1 |
| **Container Registry** | ghcr.io (free) | 0 |
| **Monitoring** | Self-hosted Prometheus/Grafana/OTel | 0 (compute only) |
| **Logging** | Grafana Cloud free tier (50 GB/mo) | 0 |
| **Backups** | pg_dump + S3/Storage Box | ~4-10 |

---

## Comparison Summary

| Provider | Monthly Cost | HA/Redundancy | Managed Services | EU Data Residency | Ops Complexity |
|----------|-------------|---------------|------------------|-------------------|----------------|
| **Hetzner (1 server)** | **~114** | None | None (self-hosted) | Germany/Finland | Low |
| **Hetzner (2 servers)** | **~168** | Partial | None (self-hosted) | Germany/Finland | Medium |
| **Hetzner (k3s)** | **~123** | Full (K8s) | None (self-hosted) | Germany/Finland | High |
| **DigitalOcean** | **~491** | Partial | PG, Redis, Search | Amsterdam/Frankfurt | Low |
| **Google Cloud** | **~450-618** | Full | PG, Redis, K8s | Belgium/Netherlands | Medium |
| **Azure** | **~654** | Full | PG, Redis, K8s | Netherlands | Medium |
| **AWS** | **~600-868** | Full | Everything | Ireland/Frankfurt | Medium |

---

## Scaling Cost Projections

As the platform grows, costs scale primarily with compute (more replicas) and storage:

| Scale | Users | Concurrent Auctions | Hetzner | DigitalOcean | AWS |
|-------|-------|---------------------|---------|-------------|-----|
| **Launch** | 100-500 | 10-50 | ~114-168 | ~491 | ~868 |
| **Growth** | 500-2,000 | 50-200 | ~250-350 | ~700 | ~1,200 |
| **Scale** | 2,000-10,000 | 200-1,000 | ~500-800 | ~1,200 | ~2,500 |
| **Enterprise** | 10,000+ | 1,000+ | ~1,000-2,000 | ~2,500 | ~5,000+ |

Growth assumptions: 2x replicas per service tier, larger DB instances, more storage, CDN egress.

---

## Recommendation

### For Launch: Hetzner Single Dedicated Server (~EUR 114/mo)

**Why:**
- 80-90% cheaper than hyperscalers for equivalent compute
- EU data residency (German datacenters, GDPR-compliant)
- Unlimited bandwidth (critical for image-heavy auction platform)
- Docker Compose already works - deploy immediately
- 128 GB RAM gives massive headroom to grow
- Upgrade path is clear: add second server when needed

**When to move to Kubernetes:**
- When you need zero-downtime deployments
- When traffic requires auto-scaling (HPA)
- When you have a dedicated DevOps engineer
- Still use Hetzner (k3s) - your Helm charts are ready

**When to move to AWS/GCP:**
- When you need managed services to reduce ops burden
- When compliance requires specific cloud certifications (SOC 2, ISO 27001)
- When you need multi-region deployment
- Consider 1-year reserved instances to cut costs 30-40%

### Cost Savings Tips

1. **Reserved Instances** (AWS/GCP/Azure): 30-60% savings on 1-3 year commitments
2. **ARM instances** (Hetzner CAX, AWS Graviton): 20-40% cheaper, Java 21 supports ARM natively
3. **Spot/preemptible nodes**: 60-90% savings for stateless services (not for databases)
4. **Single PostgreSQL instance**: 14 databases on 1 instance is fine - no need for 14 managed instances
5. **Cloudflare CDN**: Free tier eliminates most egress costs
6. **Self-host monitoring**: Prometheus + Grafana + Loki is free (vs Datadog at EUR 195-600+/mo)
7. **Hetzner Server Auction**: Refurbished servers at 20-40% discount (same hardware, same SLA)
