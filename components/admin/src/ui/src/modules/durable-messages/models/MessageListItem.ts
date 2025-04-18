import { DateTime } from "luxon"

export type MessageListItem = {
  messageId: string
  numFailures: number
  createdAt: DateTime,
  firstScheduledFor: DateTime
  nextScheduledFor: DateTime | null
  stateChangedAt: DateTime
}
