import { DateTime } from "luxon"

export type WorkItemListItem = {
  itemId: string
  numFailures: number
  createdAt: DateTime,
  firstScheduledFor: DateTime
  nextScheduledFor: DateTime | null
  stateChangedAt: DateTime
}
