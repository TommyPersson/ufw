import { Command } from "../../common/utils/commands"
import { WorkItemDetails, WorkItemFailure, WorkItemListItem, WorkQueueDetails } from "./common/models"

export type WorkItemCommandArgs = { queueId: string, itemId: string }

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
  },
  texts: {
    queueTypeSingular: string
  },
  links: {
    workItemDetailsLinkFormatter: (queueId: string, itemId: string) => string
  }
}