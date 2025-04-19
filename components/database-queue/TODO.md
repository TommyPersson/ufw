* [x] Hanged item re-scheduler
* [ ] Tracing
* [x] ConsumerSignal
* [ ] Analogue of JobOwnershipLostException
* [ ] "in-order" processing using queue locks
* [ ] Timeline UI
* [ ] DB Indexes 


--CREATE INDEX ufw__db_queue__items__state__idx ON ufw__db_queue__items (state);

--CREATE INDEX ufw__db_queue__items__next_scheduled_for__idx ON ufw__db_queue__items (next_scheduled_for)
--    WHERE next_scheduled_for IS NOT NULL;

--CREATE INDEX ufw__db_queue__items__expires_at__idx ON ufw__db_queue__items (expires_at)
--    WHERE expires_at IS NOT NULL;

--CREATE INDEX ufw__db_queue__items__watchdog_timestamp__idx ON ufw__db_queue__items (watchdog_timestamp)
--    WHERE watchdog_timestamp IS NOT NULL;

--CREATE INDEX ufw__db_queue__items__concurrency_key__idx ON ufw__db_queue__items (concurrency_key)
--    WHERE concurrency_key IS NOT NULL;

--CREATE INDEX ufw__db_queue__failures__item_uid__idx ON ufw__db_queue__failures (item_uid);