import CheckIcon from "@mui/icons-material/Check"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"
import { FeatureToggleListQueryKeyPrefix } from "../queries"

export const EnableFeatureToggleCommand: Command<{ featureToggleId: string }> = {
  mutationOptions: ({
    mutationKey: ["feature-toggle", "EnableFeatureToggle"],
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
  icon: <CheckIcon />,
  errorTitle: "Unable to enable feature toggle",
  confirmText: ({ featureToggleId }) =>
    <>Are you sure you want to <strong>enable</strong> the <code>{featureToggleId}</code> feature toggle?</>,
  confirmColor: "primary",
}
