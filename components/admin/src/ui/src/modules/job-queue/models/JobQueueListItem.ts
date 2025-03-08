
export type JobQueueListItem = {
  queueId: string
  numScheduled: number
  numPending: number
  numInProgress: number
  numFailed: number
}