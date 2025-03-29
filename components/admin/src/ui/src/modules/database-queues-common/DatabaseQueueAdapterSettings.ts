import { Command } from "../../common/utils/commands"
import { WorkItemDetails, WorkItemFailure, WorkItemListItem, WorkItemState, WorkQueueDetails } from "./models"

export type WorkItemCommandArgs = { queueId: string, itemId: string }
export type WorkQueueCommandArgs = { queueId: string }

export interface DatabaseQueueAdapterSettings<
  TQueueDetails = any,
  TItemListItem = any,
  TItemDetails = any,
  TItemFailure = any,
  TWorkItemCommandArgs = any,
> {
  transforms: {
    queueDetailsTransform: (it: TQueueDetails) => WorkQueueDetails
    itemListItemTransform: (it: TItemListItem) => WorkItemListItem
    itemDetailsTransform: (it: TItemDetails) => WorkItemDetails
    failureTransform: (it: TItemFailure) => WorkItemFailure
  },
  commands: {
    rescheduleItemNowCommand: Command<TWorkItemCommandArgs>
    rescheduleItemNowArgsTransform: (it: WorkItemCommandArgs) => TWorkItemCommandArgs
    deleteItemCommand: Command<TWorkItemCommandArgs>
    deleteItemArgsTransform: (it: WorkItemCommandArgs) => TWorkItemCommandArgs
    cancelItemCommand: Command<TWorkItemCommandArgs>
    cancelItemArgsTransform: (it: WorkItemCommandArgs) => TWorkItemCommandArgs
    deleteAllFailedItemsCommand: Command<WorkQueueCommandArgs>
    rescheduleAllFailedItemsCommand: Command<WorkQueueCommandArgs>
    pauseWorkQueueCommand: Command<WorkQueueCommandArgs>
    unpauseWorkQueueCommand: Command<WorkQueueCommandArgs>
  },
  texts: {
    queueTypeSingular: string
    queueTypePlural: string
  },
  linkFormatters: {
    workQueueDetails: (queueId: string) => string
    workItemList: (queueId: string, state: WorkItemState) => string
    workItemDetails: (queueId: string, itemId: string) => string
  }
}