import { AlertColor } from "@mui/material"
import { JobQueueState } from "../../models/JobQueueState"

export function getQueueStateColor(state: JobQueueState): AlertColor {
  switch (state) {
    case "ACTIVE":
      return "success"
    case "PAUSED":
      return "error"
    default:
      return "info"
  }
}