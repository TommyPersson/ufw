import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"
import { FeatureToggleListQueryKeyPrefix } from "../queries"

export const DisableFeatureToggleCommand: Command<{ featureToggleId: string }> = {
  mutationOptions: ({
    mutationKey: ["feature-toggle", "DisableFeatureToggle"],
    mutationFn: async ({ featureToggleId }) => {
      await makeApiRequest(`/admin/api/feature-toggles/feature-toggles/${featureToggleId}/actions/disable`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      queryClient.invalidateQueries({ queryKey: FeatureToggleListQueryKeyPrefix }).then()
    }
  }),
  label: "Disable",
  errorTitle: "Unable to disable feature toggle",
  confirmText: ({ featureToggleId }) =>
    <>Are you sure you want to <strong>disable</strong> the <code>{featureToggleId}</code> feature toggle?</>,
  confirmColor: "error",
}
