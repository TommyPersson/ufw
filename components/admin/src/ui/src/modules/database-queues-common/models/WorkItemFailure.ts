import { DateTime } from "luxon"

export type WorkItemFailure = {
  failureId: string
  workItemId: string
  timestamp: DateTime
  errorType: string
  errorMessage: string
  errorStackTrace: string
}
