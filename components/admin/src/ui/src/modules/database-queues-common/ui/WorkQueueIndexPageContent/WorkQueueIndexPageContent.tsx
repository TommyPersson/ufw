import WarningIcon from "@mui/icons-material/Warning"
import {
  Box,
  Card,
  CardContent,
  Chip,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip
} from "@mui/material"
import { uniqBy } from "es-toolkit"
import { ApplicationModuleHeader, DateTimeText, ErrorAlert, LinkTableCell } from "../../../../common/components"
import { ApplicationModule } from "../../../../common/models"
import { WorkQueueListItem } from "../../models"
import { DatabaseQueueAdapterSettings } from "../../DatabaseQueueAdapterSettings"
import { getQueueStateColor } from "../utils/colors"

import classes from "./WorkQueueIndexPageContent.module.css"

export const WorkQueueIndexPageContent = (props: {
  queues: WorkQueueListItem[]
  error: Error | null
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { queues, error, adapterSettings } = props

  const applicationModules = uniqBy(
    queues.map(it => it.applicationModule),
    (it) => it.id
  )

  return (
    <>
      <ErrorAlert error={error} />
      {applicationModules.map(module => {
        const queuesInModule = queues.filter(it => it.applicationModule.id === module.id)
        return <QueuesTableCard
          key={module.id}
          module={module}
          queues={queuesInModule}
          adapterSettings={adapterSettings}
        />
      })}
    </>
  )
}


const QueuesTableCard = (props: {
  queues: WorkQueueListItem[]
  module: ApplicationModule
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { queues, module, adapterSettings } = props

  return (
    <TableContainer component={Card}>
      <CardContent>
        <ApplicationModuleHeader applicationModule={module} />
      </CardContent>
      <Divider />
      <Table className={classes.Table}>
        <TableHead>
          <TableRow>
            <TableCell style={{ width: 0 }}></TableCell>
            <TableCell>Queue ID</TableCell>
            <TableCell style={{ width: 0 }}>State</TableCell>
            <TableCell style={{ width: 0 }}># Scheduled</TableCell>
            <TableCell style={{ width: 0 }}># Pending</TableCell>
            <TableCell style={{ width: 0 }}># In Progress</TableCell>
            <TableCell style={{ width: 0 }}># Failed</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          <>
            {queues.map(it =>
              <QueueTableRow key={it.queueId} queue={it} adapterSettings={adapterSettings} />
            )}
          </>
        </TableBody>
      </Table>
    </TableContainer>
  )
}

const QueueTableRow = (props: {
  queue: WorkQueueListItem
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { queue, adapterSettings } = props

  const hasFailures = queue.numFailed > 0
  const link = adapterSettings.linkFormatters.workQueueDetails(queue.queueId)

  return (
    <TableRow hover key={queue.queueId}>
      <LinkTableCell to={link}>
        <Box width={24} height={24}>
          <WarningIcon
            color={"warning"}
            titleAccess={"The queue contains failed jobs"}
            style={{ visibility: hasFailures ? "visible" : "hidden" }}
          />
        </Box>
      </LinkTableCell>
      <LinkTableCell to={link}><code>{queue.queueId}</code></LinkTableCell>
      <LinkTableCell to={link}><QueueStatusChip queue={queue} /></LinkTableCell>
      <LinkTableCell to={link}>{queue.numScheduled}</LinkTableCell>
      <LinkTableCell to={link}>{queue.numPending}</LinkTableCell>
      <LinkTableCell to={link}>{queue.numInProgress}</LinkTableCell>
      <LinkTableCell to={link}>{queue.numFailed}</LinkTableCell>
    </TableRow>
  )
}

const QueueStatusChip = (props: { queue: WorkQueueListItem }) => {
  const { queue } = props
  const status = queue.status
  const state = status.state

  const tooltip = state === "PAUSED"
    ? <>{state} since <DateTimeText dateTime={status.stateChangedAt} /></>
    : null

  return (
    <Tooltip title={tooltip}>
      <Chip
        size={"small"}
        color={getQueueStateColor(state)}
        variant={"outlined"}
        label={state}
      />
    </Tooltip>
  )
}