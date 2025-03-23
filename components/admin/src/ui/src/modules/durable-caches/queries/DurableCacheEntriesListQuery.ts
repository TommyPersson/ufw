import { UseQueryOptions } from "@tanstack/react-query"
import { makeApiRequest, PaginatedList } from "../../../common/utils/api"
import { zx } from "../../../common/utils/zod"
import { DurableCacheEntryItem, durableCacheEntryItemSchema } from "../models"

export const DurableCacheEntriesListQueryKeyPrefix = ["durable-caches", "caches", "by-id", "entries"]

export const DurableCacheEntriesListQuery: (cacheId: string, keyPrefix: string, page: number) => UseQueryOptions<PaginatedList<DurableCacheEntryItem>> =
  (cacheId, keyPrefix, page) => ({
    queryKey: [...DurableCacheEntriesListQueryKeyPrefix, cacheId, keyPrefix, page],
    queryFn: async () => {
      if (keyPrefix.length === 0) {
        return {
          items: [],
          hasMoreItems: false,
        }
      }

      const limit = 100
      const offset = (page - 1) * 100
      return responseSchema.parse(await makeApiRequest(`/admin/api/durable-caches/caches/${cacheId}/entries?keyPrefix=${keyPrefix}&limit=${limit}&offset=${offset}`))
    },
  })

const responseSchema = zx.paginatedList(
  durableCacheEntryItemSchema
)
