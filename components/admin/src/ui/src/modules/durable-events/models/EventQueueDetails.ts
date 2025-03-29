import { z } from "zod"
import { applicationModuleSchema } from "../../../common/models"
import { eventQueueStatusSchema } from "./EventQueueStatus"


export const eventQueueDetailsSchema = z.object({
  queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  status: eventQueueStatusSchema,
  eventTypes: z.object({
    typeName: z.string(),
    className: z.string(),
    description: z.string().nullable(),
  }).array(),
  applicationModule: applicationModuleSchema,
})

export type EventQueueDetails = z.infer<typeof eventQueueDetailsSchema>