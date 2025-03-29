import { DateTime } from "luxon"
import { WorkItemState } from "./WorkItemState"

export type WorkItemDetails = {
  queueId: string
  itemId: string
  itemType: string
  itemTypeClass: string
  itemTypeDescription: string
  state: WorkItemState
  dataJson: string
  metadataJson: string
  concurrencyKey: string | null
  createdAt: DateTime,
  firstScheduledFor: DateTime
  nextScheduledFor: DateTime | null
  stateChangedAt: DateTime
  watchdogTimestamp: DateTime | null
  watchdogOwner: string | null
  numFailures: number
  expiresAt: DateTime | null
}