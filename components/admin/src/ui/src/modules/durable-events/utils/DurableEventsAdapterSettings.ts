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
  CancelEventCommand,
  DeleteAllFailedEventsCommand,
  DeleteEventCommand,
  PauseEventQueueCommand,
  RescheduleAllFailedEventsCommand,
  RescheduleEventNowCommand,
  UnpauseEventQueueCommand
} from "../commands"
import { EventDetails, EventFailure, EventListItem, EventQueueDetails } from "../models"

const workItemArgsTransform = (it: WorkItemCommandArgs) => ({ queueId: it.queueId, eventId: it.itemId })

export const DurableEventsAdapterSettings: DatabaseQueueAdapterSettings<
  EventQueueDetails,
  EventListItem,
  EventDetails,
  EventFailure
> = {
  transforms: {
    queueDetailsTransform: (it) => ({
      ...it,
      workItemTypes: it.eventTypes
    }),
    itemListItemTransform: (it: EventListItem): WorkItemListItem => ({
      ...it,
      itemId: it.eventId
    }),
    failureTransform: (it: EventFailure): WorkItemFailure => ({
      ...it,
      workItemId: it.eventId
    }),
    itemDetailsTransform: (it: EventDetails): WorkItemDetails => ({
      ...it,
      // TODO prefix queueId? or pretend its the same? The API will still use event queue IDs
      itemId: it.eventId,
      itemType: it.eventType,
      itemTypeClass: it.eventTypeClass,
      itemTypeDescription: it.eventTypeDescription,
    }),
  },
  commands: {
    rescheduleItemNowCommand: RescheduleEventNowCommand,
    rescheduleItemNowArgsTransform: workItemArgsTransform,
    deleteItemCommand: DeleteEventCommand,
    deleteItemArgsTransform: workItemArgsTransform,
    cancelItemCommand: CancelEventCommand,
    cancelItemArgsTransform: workItemArgsTransform,
    rescheduleAllFailedItemsCommand: RescheduleAllFailedEventsCommand,
    deleteAllFailedItemsCommand: DeleteAllFailedEventsCommand,
    pauseWorkQueueCommand: PauseEventQueueCommand,
    unpauseWorkQueueCommand: UnpauseEventQueueCommand,
  },
  texts: {
    queueTypeSingular: "Event",
    queueTypePlural: "Events",
  },
  linkFormatters: {
    workQueueDetails: (queueId) =>
      `/durable-events/queues/${queueId}/details`,
    workItemList: (queueId, state) =>
      `/durable-events/queues/${queueId}/events/${state}`,
    workItemDetails: (queueId, itemId) =>
      `/durable-events/queues/${queueId}/events/by-id/${itemId}/details`,
  }
}