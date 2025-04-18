import { DateTime } from "luxon"

export type MessageFailure = {
  failureId: string
  messageId: string
  timestamp: DateTime
  errorType: string
  errorMessage: string
  errorStackTrace: string
}
