import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { EventListItem, EventState } from "../models"

export const EventListQuery: (queueId: string, eventState: EventState, page: number) => UseQueryOptions<PaginatedList<EventListItem>> =
  (queueId, eventState, page) => ({
    queryKey: ["durable-events", "queues", queueId, "events", eventState, page],
    queryFn: async () => {
      const limit = 100
      const offset = (page - 1) * 100
      return responseSchema.parse(await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/events?state=${eventState}&limit=${limit}&offset=${offset}`))
    },
  })

const itemSchema = z.object({
  eventId: z.string(),
  eventType: z.string(),
  numFailures: z.number(),
  createdAt: zx.dateTime,
  firstScheduledFor: zx.dateTime,
  nextScheduledFor: zx.dateTime.nullable(),
  stateChangedAt: zx.dateTime,
})

const responseSchema = zx.paginatedList(
  itemSchema
)
