export type WorkQueueDetails = {
  queueId: string
  numScheduled: number
  numPending: number
  numInProgress: number
  numFailed: number
  // TODO
}

/*
queueId: z.string(),
  numScheduled: z.number(),
  numPending: z.number(),
  numInProgress: z.number(),
  numFailed: z.number(),
  status: jobQueueStatusSchema,
  jobTypes: z.object({
  type: z.string(),
  jobClassName: z.string(),
  description: z.string().nullable(),
}).array(),
  applicationModule: applicationModuleSchema,

 */