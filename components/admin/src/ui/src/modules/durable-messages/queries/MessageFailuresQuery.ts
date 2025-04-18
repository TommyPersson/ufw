import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { MessageFailure } from "../models/MessageFailure"

export const MessageFailuresQuery: (queueId: string, messageId: string) => UseQueryOptions<PaginatedList<MessageFailure>> =
  (queueId, messageId) => ({
    queryKey: ["durable-messages", "queues", queueId, "message", messageId, "failures"],
    queryFn: async () => {
      return responseSchema.parse(await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/messages/${messageId}/failures?limit=5`))
    },
  })

const itemSchema = z.object({
  failureId: z.string(),
  messageId: z.string(),
  timestamp: zx.dateTime,
  errorType: z.string(),
  errorMessage: z.string(),
  errorStackTrace: z.string(),
})

const responseSchema = zx.paginatedList(
  itemSchema
)
