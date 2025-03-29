import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { EventFailure } from "../models/EventFailure"

export const EventFailuresQuery: (queueId: string, eventId: string) => UseQueryOptions<PaginatedList<EventFailure>> =
  (queueId, eventId) => ({
    queryKey: ["durable-events", "queues", queueId, "event", eventId, "failures"],
    queryFn: async () => {
      return responseSchema.parse(await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/events/${eventId}/failures?limit=5`))
    },
  })

const itemSchema = z.object({
  failureId: z.string(),
  eventId: z.string(),
  timestamp: zx.dateTime,
  errorType: z.string(),
  errorMessage: z.string(),
  errorStackTrace: z.string(),
})

const responseSchema = zx.paginatedList(
  itemSchema
)
