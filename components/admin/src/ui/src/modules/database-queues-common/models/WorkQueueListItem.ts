import { ApplicationModule } from "../../../common/models"
import { WorkQueueStatus } from "./WorkQueueStatus"

export type WorkQueueListItem = {
  queueId: string
  numScheduled: number
  numPending: number
  numInProgress: number
  numFailed: number
  status: WorkQueueStatus
  applicationModule: ApplicationModule
}

