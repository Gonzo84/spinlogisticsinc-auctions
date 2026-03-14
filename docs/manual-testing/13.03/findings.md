### 13.03
- **Admin portal** /lots/approval url , lot approval Queue, Description is missing (see @/home/radionica/Radionica/Tradex/Tradex/eu-auction-platform/docs/manual-testing/13.03/lot-approval-description.png)
- **Buyer portal** search url, active filter clear all action is not working (see @/home/radionica/Radionica/Tradex/Tradex/eu-auction-platform/docs/manual-testing/13.03/clear-all.png)
- **Buyer portal** get request on http://localhost:8080/api/v1/auctions?featured=true&status=ACTIVE&size=12 returns {
  "status": 500,
  "title": "Internal Server Error",
  "instance": "/api/v1/auctions"
  }
