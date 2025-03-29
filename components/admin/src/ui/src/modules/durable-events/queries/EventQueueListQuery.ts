import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { EventQueueListItem, eventQueueListItemSchema } from "../models"

export const EventQueueListQuery: UseQueryOptions<EventQueueListItem[]> = {
  queryKey: ["durable-events", "queues"],
  queryFn: async () => {
    return responseSchema.parse(await makeApiRequest("/admin/api/durable-events/queues"))
  },
}


const responseSchema = z.array(
  eventQueueListItemSchema
)