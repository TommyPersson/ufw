import PlayArrowIcon from "@mui/icons-material/PlayArrow"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const UnpauseEventQueueCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-events', 'UnpauseEventQueue'],
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-events/queues/${queueId}/actions/unpause`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-events", "queues"] })
    }
  }),
  label: "Unpause",
  color: "primary",
  icon: <PlayArrowIcon />,
  errorTitle: "Unable to unpause queue",
}
