import CloseIcon from "@mui/icons-material/Close"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const InvalidateCacheEntryCommand: Command<{ cacheId: string, cacheKey: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-caches", "InvalidateCacheEntry"],
    mutationFn: async ({ cacheId, cacheKey }) => {
      await makeApiRequest(`/admin/api/durable-caches/caches/${cacheId}/entries/${cacheKey}/actions/invalidate`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      queryClient.invalidateQueries({ queryKey: ["durable-caches"] }).then()
    }
  }),
  label: "Invalidate",
  icon: <CloseIcon />,
  color: "error",
  errorTitle: "Unable to invalidate entry",
  confirmText: ({ cacheKey }) =>
    <>Are you sure you want to <strong>invalidate</strong> the cache entry for <code>{cacheKey}</code>?</>,
  confirmColor: "error",
}
