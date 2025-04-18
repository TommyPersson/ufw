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
  CancelMessageCommand,
  DeleteAllFailedMessagesCommand,
  DeleteMessageCommand,
  PauseMessageQueueCommand,
  RescheduleAllFailedMessagesCommand,
  RescheduleMessageNowCommand,
  UnpauseMessageQueueCommand
} from "../commands"
import { MessageDetails, MessageFailure, MessageListItem, MessageQueueDetails } from "../models"

const workItemArgsTransform = (it: WorkItemCommandArgs) => ({ queueId: it.queueId, messageId: it.itemId })

export const DurableMessagesAdapterSettings: DatabaseQueueAdapterSettings<
  MessageQueueDetails,
  MessageListItem,
  MessageDetails,
  MessageFailure
> = {
  transforms: {
    queueDetailsTransform: (it) => ({
      ...it,
      workItemTypes: it.messageTypes
    }),
    itemListItemTransform: (it: MessageListItem): WorkItemListItem => ({
      ...it,
      itemId: it.messageId
    }),
    failureTransform: (it: MessageFailure): WorkItemFailure => ({
      ...it,
      workItemId: it.messageId
    }),
    itemDetailsTransform: (it: MessageDetails): WorkItemDetails => ({
      ...it,
      // TODO prefix queueId? or pretend its the same? The API will still use message queue IDs
      itemId: it.messageId,
      itemType: it.messageType,
      itemTypeClass: it.messageTypeClass,
      itemTypeDescription: it.messageTypeDescription,
    }),
  },
  commands: {
    rescheduleItemNowCommand: RescheduleMessageNowCommand,
    rescheduleItemNowArgsTransform: workItemArgsTransform,
    deleteItemCommand: DeleteMessageCommand,
    deleteItemArgsTransform: workItemArgsTransform,
    cancelItemCommand: CancelMessageCommand,
    cancelItemArgsTransform: workItemArgsTransform,
    rescheduleAllFailedItemsCommand: RescheduleAllFailedMessagesCommand,
    deleteAllFailedItemsCommand: DeleteAllFailedMessagesCommand,
    pauseWorkQueueCommand: PauseMessageQueueCommand,
    unpauseWorkQueueCommand: UnpauseMessageQueueCommand,
  },
  texts: {
    queueTypeSingular: "Message",
    queueTypePlural: "Messages",
  },
  linkFormatters: {
    workQueueDetails: (queueId) =>
      `/durable-messages/queues/${queueId}/details`,
    workItemList: (queueId, state) =>
      `/durable-messages/queues/${queueId}/messages/${state}`,
    workItemDetails: (queueId, itemId) =>
      `/durable-messages/queues/${queueId}/messages/by-id/${itemId}/details`,
  }
}