import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { EventDetails, eventStateSchema } from "../models"

export const EventDetailsQuery: (queueId: string, eventId: string) => UseQueryOptions<EventDetails | null> = (queueId, eventId) => ({
  queryKey: ["durable-events", "queues", queueId, "event", eventId, "details"],
  queryFn: async () => {
    return detailsSchema.parse(await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/events/${eventId}/details`))
  },
})

const detailsSchema = z.object({
  eventId: z.string(),
  queueId: z.string(),
  eventType: z.string(),
  eventTypeClass: z.string(),
  eventTypeDescription: z.string().nullable(),
  state: eventStateSchema,
  dataJson: z.string(),
  metadataJson: z.string(),
  concurrencyKey: z.string().nullable(),
  createdAt: zx.dateTime,
  firstScheduledFor: zx.dateTime,
  nextScheduledFor: zx.dateTime.nullable(),
  stateChangedAt: zx.dateTime,
  watchdogTimestamp: zx.dateTime.nullable(),
  watchdogOwner: z.string().nullable(),
  numFailures: z.number(),
  expiresAt: zx.dateTime.nullable(),
})
