import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { DurableCacheDetails, durableCacheItemSchema } from "../models"

export const DurableCacheDetailsQueryKeyPrefix = ["durable-caches", "caches", "by-id"]

export const DurableCacheDetailsQuery: (cacheId: string) => UseQueryOptions<DurableCacheDetails> =
  (cacheId) => ({
    queryKey: [...DurableCacheDetailsQueryKeyPrefix, cacheId],
    queryFn: async () => {
      return durableCacheItemSchema.parse(await makeApiRequest(`/admin/api/durable-caches/caches/${cacheId}`))
    },
  })