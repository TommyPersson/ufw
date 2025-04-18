import PlaylistPlayIcon from "@mui/icons-material/PlaylistPlay"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const RescheduleMessageNowCommand: Command<{ queueId: string, messageId: string }> = {
  mutationOptions: ({
    mutationKey: ["durable-messages", "RescheduleMessageNow"],
    mutationFn: async ({ queueId, messageId }) => {
      await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/messages/${messageId}/actions/reschedule-now`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      queryClient.invalidateQueries({ queryKey: ["durable-messages", "queues"] }).then()
    }
  }),
  label: "Reschedule message",
  color: "primary",
  icon: <PlaylistPlayIcon />,
  errorTitle: "Unable to reschedule message",
}
