import PlayArrowIcon from "@mui/icons-material/PlayArrow"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const UnpauseJobQueueCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-jobs', 'UnpauseJobQueue'],
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/actions/unpause`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-jobs"] })
    }
  }),
  label: "Unpause",
  color: "primary",
  icon: <PlayArrowIcon />,
  errorTitle: "Unable to unpause queue",
}
