import CloseIcon from "@mui/icons-material/Close"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const InvalidateAllCacheEntriesCommand: Command<{ cacheId: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-caches", "InvalidateAllCacheEntries"],
    mutationFn: async ({ cacheId }) => {
      await makeApiRequest(`/admin/api/durable-caches/caches/${cacheId}/actions/invalidate-all`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      queryClient.invalidateQueries({ queryKey: ["durable-caches"] }).then()
    }
  }),
  label: "Invalidate All Entries",
  icon: <CloseIcon />,
  color: "error",
  errorTitle: "Unable to invalidate entries",
  confirmText: ({ cacheId }) =>
    <>Are you sure you want to <strong>invalidate all</strong> the cache entries of <code>{cacheId}</code>?</>,
  confirmColor: "error",
}
