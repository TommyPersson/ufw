import WarningIcon from "@mui/icons-material/Warning"
import {
  Box,
  Chip,
  Paper,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip
} from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { Link } from "react-router"
import { DateTimeText, ErrorAlert, Page, PageBreadcrumb } from "../../../../common/components"
import { JobQueueListItem } from "../../models"
import { JobQueueListQuery } from "../../queries"
import { getQueueStateColor } from "../utils/colors"

import classes from "./JobQueueIndexPage.module.css"

export const JobQueueIndexPage = () => {
  const queuesQuery = useQuery(JobQueueListQuery)

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
      <Paper>
        <TableContainer>
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
              {queuesQuery.isLoading && (
                <TableRow>
                  <TableCell>
                    <Box width={24} height={24}>
                      <WarningIcon style={{ visibility: "hidden" }} />
                    </Box>
                  </TableCell>
                  <TableCell><Skeleton variant="text" /></TableCell>
                  <TableCell><Skeleton variant="text" /></TableCell>
                  <TableCell><Skeleton variant="text" /></TableCell>
                  <TableCell><Skeleton variant="text" /></TableCell>
                  <TableCell><Skeleton variant="text" /></TableCell>
                  <TableCell><Skeleton variant="text" /></TableCell>
                </TableRow>
              )}
              {queuesQuery.data?.map(it => (
                <QueueTableRow key={it.queueId} queue={it} />
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Page>
  )
}

const QueueTableRow = (props: { queue: JobQueueListItem }) => {
  const { queue } = props

  const hasFailures = queue.numFailed > 0

  return (
    <TableRow hover key={queue.queueId}>
      <TableCell>
        <Box width={24} height={24}>
          <WarningIcon
            color={"warning"}
            titleAccess={"The queue contains failed jobs"}
            style={{ visibility: hasFailures ? "visible" : "hidden" }}
          />
        </Box>
      </TableCell>
      <TableCell><code><Link to={`queues/${queue.queueId}/details`}>{queue.queueId}</Link></code></TableCell>
      <TableCell><QueueStatusChip queue={queue} /></TableCell>
      <TableCell>{queue.numScheduled}</TableCell>
      <TableCell>{queue.numPending}</TableCell>
      <TableCell>{queue.numInProgress}</TableCell>
      <TableCell>{queue.numFailed}</TableCell>
    </TableRow>
  )
}

const QueueStatusChip = (props: { queue: JobQueueListItem })=> {
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