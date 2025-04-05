import HelpOutlineOutlinedIcon from "@mui/icons-material/HelpOutlineOutlined"
import PauseCircleOutlinedIcon from "@mui/icons-material/PauseCircleOutlined"
import PlayCircleOutlinedIcon from "@mui/icons-material/PlayCircleOutlined"
import { SvgIconProps } from "@mui/material"
import { WorkQueueState } from "../../models"

export function getQueueStateIcon(state: WorkQueueState): React.ReactElement<SvgIconProps> {
  switch (state) {
    case "ACTIVE":
      return <PlayCircleOutlinedIcon />
    case "PAUSED":
      return <PauseCircleOutlinedIcon />
    default:
      return <HelpOutlineOutlinedIcon />
  }
}