export type JobQueueDetails = {
  queueId: string
  numScheduled: number
  numPending: number
  numInProgress: number
  numFailed: number
  jobTypes: JobType[]
}

export type JobType = {
  type: string
  jobClassName: string
  description: string | null
}