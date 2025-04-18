import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { MessageQueueListItem, messageQueueListItemSchema } from "../models"

export const MessageQueueListQuery: UseQueryOptions<MessageQueueListItem[]> = {
  queryKey: ["durable-messages", "queues"],
  queryFn: async () => {
    return responseSchema.parse(await makeApiRequest("/admin/api/durable-messages/queues"))
  },
}


const responseSchema = z.array(
  messageQueueListItemSchema
)