import { DateTime } from "luxon"

export type JobFailure = {
  failureId: string
  jobId: string
  timestamp: DateTime
  errorType: string
  errorMessage: string
  errorStackTrace: string
}
