import { DateTime } from "luxon"

import { WorkQueueState } from "./WorkQueueState"

export type WorkQueueStatus = {
  state: WorkQueueState
  stateChangedAt: DateTime
}