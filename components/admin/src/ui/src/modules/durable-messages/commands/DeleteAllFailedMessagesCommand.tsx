import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"

export const DeleteAllFailedMessagesCommand: Command<{ queueId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-messages', 'DeleteAllFailedMessages'],
    mutationFn: async ({ queueId }) => {
      await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/actions/delete-all-failed-messages`, {
        method: "POST"
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["durable-messages", "queues"] })
    }
  }),
  label: "Delete all failed messages",
  color: "error",
  icon: <DeleteOutlineIcon />,
  errorTitle: "Unable to delete messages",
  confirmText: ({ queueId }) =>
    <>Are you sure you want to <strong>delete</strong> all failed messages in the <code>{queueId}</code> queue?</>,
  confirmColor: "error",
}
