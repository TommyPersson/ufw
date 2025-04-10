import { z } from "zod";
import { zx } from "../../../common/utils/zod";

export const clusterInstanceSchema = z.object({
  instanceId: z.string(),
  appVersion: z.string(),
  startedAt: zx.dateTime,
  heartbeatTimestamp: zx.dateTime,
})

export type ClusterInstance = z.infer<typeof clusterInstanceSchema>