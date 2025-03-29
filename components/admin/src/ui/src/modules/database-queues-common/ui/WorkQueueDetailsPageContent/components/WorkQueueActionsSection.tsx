import { Box } from "@mui/material"
import { CommandButton, PageSectionHeader } from "../../../../../common/components"
import { DatabaseQueueAdapterSettings } from "../../../DatabaseQueueAdapterSettings"

export const WorkQueueActionsSection = (props: {
  queueId: string
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { queueId, adapterSettings } = props
  return (
    <>
      <PageSectionHeader>Actions</PageSectionHeader>
      <Box sx={{ display: "flex", gap: 2, flexDirection: "column" }}>
        <CommandButton
          variant={"contained"}
          sx={{ alignSelf: "flex-start" }}
          command={adapterSettings.commands.rescheduleAllFailedItemsCommand}
          args={{ queueId }}
        />
        <CommandButton
          variant={"contained"}
          sx={{ alignSelf: "flex-start" }}
          command={adapterSettings.commands.deleteAllFailedItemsCommand}
          args={{ queueId }}
        />
      </Box>
    </>
  )
}
