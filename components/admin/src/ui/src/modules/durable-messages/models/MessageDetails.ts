import { DateTime } from "luxon"
import { MessageState } from "./MessageState"

export type MessageDetails = {
  queueId: string
  messageId: string
  messageType: string
  messageTypeClass: string
  messageTypeDescription: string | null
  state: MessageState
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
