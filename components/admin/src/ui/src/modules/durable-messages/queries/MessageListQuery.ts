import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { MessageListItem, MessageState } from "../models"

export const MessageListQuery: (queueId: string, messageState: MessageState, page: number) => UseQueryOptions<PaginatedList<MessageListItem>> =
  (queueId, messageState, page) => ({
    queryKey: ["durable-messages", "queues", queueId, "messages", messageState, page],
    queryFn: async () => {
      const limit = 100
      const offset = (page - 1) * 100
      return responseSchema.parse(await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/messages?state=${messageState}&limit=${limit}&offset=${offset}`))
    },
  })

const itemSchema = z.object({
  messageId: z.string(),
  messageType: z.string(),
  numFailures: z.number(),
  createdAt: zx.dateTime,
  firstScheduledFor: zx.dateTime,
  nextScheduledFor: zx.dateTime.nullable(),
  stateChangedAt: zx.dateTime,
})

const responseSchema = zx.paginatedList(
  itemSchema
)
