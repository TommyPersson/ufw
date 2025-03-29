import { DateTime } from "luxon"

export type EventFailure = {
  failureId: string
  eventId: string
  timestamp: DateTime
  errorType: string
  errorMessage: string
  errorStackTrace: string
}
