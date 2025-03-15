import { DateTime } from "luxon"

export type JobListItem = {
  jobId: string
  numFailures: number
  createdAt: DateTime,
  firstScheduledFor: DateTime
  nextScheduledFor: DateTime | null
  stateChangedAt: DateTime
}
