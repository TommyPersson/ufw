
* [ ] Cache the toggle value in-memory a short while (10 seconds?)
* [ ] Allow the toggle to be "marked for deletion"
  * Such a toggle will be deleted after a fixed time (TTL), but the TTL will be removed 
    if the feature toggle is ever read again 
* [ ] Alternative: List "stale" toggles in the UI, and allow direct deletion 