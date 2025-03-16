import { DateTime } from "luxon"
import { JobState } from "./JobState"

export type JobDetails = {
  queueId: string
  jobId: string
  jobType: string
  state: JobState
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
