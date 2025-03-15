import { Tooltip } from "@mui/material"
import { DateTime } from "luxon"

export type DateTimeTextProps = {
  dateTime: DateTime | null,
  fallback?: any
}

export const DateTimeText = (props: DateTimeTextProps) => {
  const { dateTime, fallback } = props
  const timeText = dateTime ? (
    <Tooltip title={<kbd>{dateTime.toUTC().toJSON()}</kbd>}>
      <span>{dateTime.toFormat("yyyy-MM-dd HH:mm:ss")}</span>
    </Tooltip>
  ) : null

  return <span>{timeText ?? fallback}</span>
}