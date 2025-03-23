import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { DurableCacheItem, durableCacheItemSchema } from "../models"

export const DurableCachesListQueryKeyPrefix = ["durable-caches", "caches"]

export const DurableCachesListQuery: (page: number) => UseQueryOptions<PaginatedList<DurableCacheItem>> =
  (page) => ({
    queryKey: [...DurableCachesListQueryKeyPrefix, page],
    queryFn: async () => {
      const limit = 100
      const offset = (page - 1) * 100
      return responseSchema.parse(await makeApiRequest(`/admin/api/durable-caches/caches?limit=${limit}&offset=${offset}`))
    },
  })

const responseSchema = zx.paginatedList(
  durableCacheItemSchema
)
