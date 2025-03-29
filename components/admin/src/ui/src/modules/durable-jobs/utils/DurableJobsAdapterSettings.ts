import {
  WorkItemDetails,
  WorkItemFailure,
  WorkItemListItem,
} from "../../database-queues-common/models"
import {
  DatabaseQueueAdapterSettings,
  WorkItemCommandArgs
} from "../../database-queues-common/DatabaseQueueAdapterSettings"
import {
  CancelJobCommand,
  DeleteAllFailedJobsCommand,
  DeleteJobCommand,
  PauseJobQueueCommand,
  RescheduleAllFailedJobsCommand,
  RescheduleJobNowCommand,
  UnpauseJobQueueCommand
} from "../commands"
import { JobDetails, JobFailure, JobListItem, JobQueueDetails } from "../models"

const workItemArgsTransform = (it: WorkItemCommandArgs) => ({ queueId: it.queueId, jobId: it.itemId })

export const DurableJobsAdapterSettings: DatabaseQueueAdapterSettings<
  JobQueueDetails,
  JobListItem,
  JobDetails,
  JobFailure
> = {
  transforms: {
    queueDetailsTransform: (it) => ({
      ...it,
      workItemTypes: it.jobTypes
    }),
    itemListItemTransform: (it: JobListItem): WorkItemListItem => ({
      ...it,
      itemId: it.jobId
    }),
    failureTransform: (it: JobFailure): WorkItemFailure => ({
      ...it,
      workItemId: it.jobId
    }),
    itemDetailsTransform: (it: JobDetails): WorkItemDetails => ({
      ...it,
      // TODO prefix queueId? or pretend its the same? The API will still use job queue IDs
      itemId: it.jobId,
      itemType: it.jobType,
      itemTypeClass: it.jobTypeClass,
      itemTypeDescription: it.jobTypeDescription,
    }),
  },
  commands: {
    rescheduleItemNowCommand: RescheduleJobNowCommand,
    rescheduleItemNowArgsTransform: workItemArgsTransform,
    deleteItemCommand: DeleteJobCommand,
    deleteItemArgsTransform: workItemArgsTransform,
    cancelItemCommand: CancelJobCommand,
    cancelItemArgsTransform: workItemArgsTransform,
    rescheduleAllFailedItemsCommand: RescheduleAllFailedJobsCommand,
    deleteAllFailedItemsCommand: DeleteAllFailedJobsCommand,
    pauseWorkQueueCommand: PauseJobQueueCommand,
    unpauseWorkQueueCommand: UnpauseJobQueueCommand,
  },
  texts: {
    queueTypeSingular: "Job",
    queueTypePlural: "Jobs",
  },
  linkFormatters: {
    workQueueDetails: (queueId) =>
      `/durable-jobs/queues/${queueId}/details`,
    workItemList: (queueId, state) =>
      `/durable-jobs/queues/${queueId}/jobs/${state}`,
    workItemDetails: (queueId, itemId) =>
      `/durable-jobs/queues/${queueId}/jobs/by-id/${itemId}/details`,
  }
}