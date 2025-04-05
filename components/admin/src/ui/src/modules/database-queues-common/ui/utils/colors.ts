import { AlertColor } from "@mui/material"
import { WorkQueueState } from "../../models"

export function getQueueStateColor(state: WorkQueueState): AlertColor {
  switch (state) {
    case "ACTIVE":
      return "success"
    case "PAUSED":
      return "error"
    default:
      return "info"
  }
}
