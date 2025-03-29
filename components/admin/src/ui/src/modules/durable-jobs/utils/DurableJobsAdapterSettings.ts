import { WorkItemDetails, WorkItemFailure } from "../../database-queues-common/common/models"
import { WorkItemListItem } from "../../database-queues-common/common/models/WorkItemListItem"
import {
  DatabaseQueueAdapterSettings,
  WorkItemCommandArgs
} from "../../database-queues-common/DatabaseQueueAdapterSettings"
import { CancelJobCommand, DeleteJobCommand, RescheduleJobNowCommand } from "../commands"
import { JobDetails, JobFailure, JobListItem, JobQueueDetails } from "../models"

const workItemArgsTransform = (it: WorkItemCommandArgs) => ({  queueId: it.queueId, jobId: it.itemId })

export const DurableJobsAdapterSettings: DatabaseQueueAdapterSettings<
  JobQueueDetails,
  JobListItem,
  JobDetails,
  JobFailure
> = {
  transforms: {
    queueDetailsTransform: (it) => ({
      ...it,
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
  },
  texts: {
    queueTypeSingular: "Job"
  },
  links: {
    workItemDetailsLinkFormatter: (queueId: string, itemId: string) =>
      `/durable-jobs/queues/${queueId}/jobs/by-id/${itemId}/details`
  }
}