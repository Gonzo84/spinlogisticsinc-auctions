### manueal testing 04.03
- **buyer portal - when the buyer is on a search route and selects one of the categories, the list is empty, even though number by category name indicates that there are some items in this category (see category-list-empty.png).
- **buyer portal - when the buyer is on a search route, current bid price is 0 on every item in a grid, even though when user click on some of the items and proceeds to a details page, current bid is more than 0 (see current-bid-0.png)
- **buyer portal - when the buyer is on a lot details page, click on a breadcrumb navigation doesn't work (see breadcrumb.png). Check all breadcrumb navigations
- **buyer portal - when the buyer is on a auction page, and there\s lest than 2 minutes remaining until the end of auction, when new bid is placed by some other user, websocket updates new price, but the counter is not reacive and it is not extended to two minutes
- **seller portal - when the seller is on a My Lots page, click on one of quick filters (for example draft) doesn't work. all lots are in the list, not filtered by the status (see lot-status-filter.png)
- **seller portal - when the seller creates a lot with uploaded pictures and clicks edit, the uploaded pictures are not there
- **admin portal - when the admin navigates to auctions, and then select any of the auctions in the list, api/v1/auctions/{auction_id}/bids/live and api/v1/auctions/{auction_id}/lots are returning 404 ("Unable to find matching target resource method")
