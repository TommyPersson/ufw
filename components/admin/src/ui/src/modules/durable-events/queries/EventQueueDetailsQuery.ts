import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { EventQueueDetails, eventQueueDetailsSchema } from "../models"

export const EventQueueDetailsQuery: (queueId: string) => UseQueryOptions<EventQueueDetails | null> = (queueId) => ({
  queryKey: ["durable-events", "queues", queueId, "details"],
  queryFn: async () => {
    return eventQueueDetailsSchema.parse(await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/details`))
  },
})