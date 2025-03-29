import { ApplicationModule } from "../../../common/models"
import { WorkItemType } from "./WorkItemType"
import { WorkQueueStatus } from "./WorkQueueStatus"

export type WorkQueueDetails = {
  queueId: string
  numScheduled: number
  numPending: number
  numInProgress: number
  numFailed: number
  status: WorkQueueStatus
  workItemTypes: WorkItemType[]
  applicationModule: ApplicationModule
}
