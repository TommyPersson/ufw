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
import { useQuery } from "@tanstack/react-query"
import { uniqBy } from "es-toolkit"
import { useMemo } from "react"
import {
  ApplicationModuleHeader,
  DateTimeText,
  ErrorAlert,
  LinkTableCell,
  Page,
  PageBreadcrumb
} from "../../../../common/components"
import { ApplicationModule } from "../../../../common/models"
import { JobQueueListItem } from "../../models"
import { JobQueueListQuery } from "../../queries"
import { getQueueStateColor } from "../utils/colors"

import classes from "./JobQueueIndexPage.module.css"

export const JobQueueIndexPage = () => {

  const queuesQuery = useQuery(JobQueueListQuery)
  const queues = queuesQuery.data ?? []

  const applicationModules = uniqBy(queues.map(it => it.applicationModule), it => it.id)

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Jobs" },
    { text: "Job Queues", current: true },
  ], [])

  return (
    <Page
      heading={"Job Queues"}
      isLoading={queuesQuery.isFetching}
      onRefresh={queuesQuery.refetch}
      breadcrumbs={breadcrumbs}
    >
      <ErrorAlert error={queuesQuery.error} />
      {applicationModules.map(module => {
        const queuesInModule = queues.filter(it => it.applicationModule.id === module.id)
        return <QueuesTableCard
          key={module.id}
          module={module}
          queues={queuesInModule}
        />
      })}
    </Page>
  )
}

const QueuesTableCard = (props: {
  queues: JobQueueListItem[]
  module: ApplicationModule
}) => {
  const { queues, module } = props

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
              <QueueTableRow key={it.queueId} queue={it} />
            )}
          </>
        </TableBody>
      </Table>
    </TableContainer>
  )
}

const QueueTableRow = (props: { queue: JobQueueListItem }) => {
  const { queue } = props

  const hasFailures = queue.numFailed > 0
  const link = `queues/${queue.queueId}/details`

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

const QueueStatusChip = (props: { queue: JobQueueListItem }) => {
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