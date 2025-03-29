import { DateTime } from "luxon"

export type EventListItem = {
  eventId: string
  numFailures: number
  createdAt: DateTime,
  firstScheduledFor: DateTime
  nextScheduledFor: DateTime | null
  stateChangedAt: DateTime
}
