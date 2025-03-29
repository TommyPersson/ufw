import { DateTime } from "luxon"
import { EventState } from "./EventState"

export type EventDetails = {
  queueId: string
  eventId: string
  eventType: string
  eventTypeClass: string
  eventTypeDescription: string | null
  state: EventState
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
