import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { MessageQueueDetails, messageQueueDetailsSchema } from "../models"

export const MessageQueueDetailsQuery: (queueId: string) => UseQueryOptions<MessageQueueDetails | null> = (queueId) => ({
  queryKey: ["durable-messages", "queues", queueId, "details"],
  queryFn: async () => {
    return messageQueueDetailsSchema.parse(await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/details`))
  },
})