import { JobType } from "./JobType"

export type JobQueueDetails = {
  queueId: string
  numScheduled: number
  numPending: number
  numInProgress: number
  numFailed: number
  jobTypes: JobType[]
}

