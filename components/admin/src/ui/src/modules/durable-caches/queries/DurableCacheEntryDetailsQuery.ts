import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest } from "../../../common/utils/api"
import { DurableCacheEntryDetails, durableCacheEntryDetailsSchema } from "../models/DurableCacheEntryDetails"

export const DurableCacheEntryDetailsQueryKeyPrefix = ["durable-caches", "cache-entries", "by-key"]

export const DurableCacheEntryDetailsQuery: (cacheId: string, cacheKey: string) => UseQueryOptions<DurableCacheEntryDetails> =
  (cacheId, cacheKey) => ({
    queryKey: [...DurableCacheEntryDetailsQueryKeyPrefix, cacheId, cacheKey],
    queryFn: async () => {
      return durableCacheEntryDetailsSchema.parse(await makeApiRequest(`/admin/api/durable-caches/caches/${cacheId}/entries/${cacheKey}/details`))
    },
    enabled: cacheKey.trim().length > 0
  })