import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"
import { FeatureToggleListQueryKeyPrefix } from "../queries"

export const EnableFeatureToggleCommand: Command<{ featureToggleId: string }> = {
  mutationOptions: ({
    mutationKey: ['feature-toggle', 'EnableFeatureToggle'],
    mutationFn: async ({ featureToggleId }) => {
      await makeApiRequest(`/admin/api/feature-toggles/feature-toggles/${featureToggleId}/actions/enable`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      queryClient.invalidateQueries({ queryKey: FeatureToggleListQueryKeyPrefix }).then()
    }
  }),
  label: "Enable",
  errorTitle: "Unable to enable feature toggle",
  confirmText: <>Are you sure you want to <strong>enable</strong> this feature toggle?</>, // TODO which toggle?
  confirmColor: "primary",
}
