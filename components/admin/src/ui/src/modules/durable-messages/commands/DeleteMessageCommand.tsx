import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline"
import { makeApiRequest } from "../../../common/utils/api"
import { Command } from "../../../common/utils/commands"
import { queryClient } from "../../../common/utils/tsq"
import { navigate } from "../../../router"

export const DeleteMessageCommand: Command<{ queueId: string, messageId: string }> = {
  mutationOptions: ({
    mutationKey: ['durable-messages', 'DeleteMessage'],
    mutationFn: async ({ queueId, messageId }) => {
      await makeApiRequest(`/admin/api/durable-messages/queues/${queueId}/messages/${messageId}/actions/delete`, {
        method: "POST"
      })
    },
    onSuccess: async (_, { queueId }) => {
      await navigate(`/durable-messages/queues/${queueId}/details`)
      queryClient.invalidateQueries({ queryKey: ["durable-messages", "queues"] }).then()
    }
  }),
  label: "Delete message",
  color: "error",
  icon: <DeleteOutlineIcon />,
  errorTitle: "Unable to delete message",
  confirmText: <>Are you sure you want to <strong>delete</strong> this message?</>,
  confirmColor: "error",
}
