import { UseQueryOptions } from "@tanstack/react-query"
import { z } from "zod"
import { makeApiRequest } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { MessageDetails, messageStateSchema } from "../models"

export const MessageDetailsQuery: (queueId: string, messageId: string) => UseQueryOptions<MessageDetails | null> = (queueId, messageId) => ({
  queryKey: ["durable-messages", "queues", queueId, "message", messageId, "details"],
  queryFn: async () => {
    return detailsSchema.parse(await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/messages/${messageId}/details`))
  },
})

const detailsSchema = z.object({
  messageId: z.string(),
  queueId: z.string(),
  messageType: z.string(),
  messageTypeClass: z.string(),
  messageTypeDescription: z.string().nullable(),
  state: messageStateSchema,
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
