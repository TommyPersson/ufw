import PauseIcon from "@mui/icons-material/Pause"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const PauseJobQueueCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-jobs', 'PauseJobQueue'],
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-jobs/queues/${queueId}/actions/pause`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-jobs"] })
    }
  }),
  label: "Pause",
  icon: <PauseIcon />,
  color: "error",
  errorTitle: "Unable to pause queue",
  confirmText: ({ queueId }) =>
    <>Are you sure you want to <strong>pause</strong> the <code>{queueId}</code> queue?</>,
}
