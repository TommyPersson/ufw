import WarningIcon from "@mui/icons-material/Warning"
import { Box, Chip, Paper, TableCell, TableContainer, TableRow, Tooltip } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import {
  DateTimeText,
  ErrorAlert,
  LinkTableCell,
  Page,
  PageBreadcrumb,
  PaginatedTable,
  TableRowSkeleton
} from "../../../../common/components"
import { JobQueueListItem } from "../../models"
import { JobQueueListQuery } from "../../queries"
import { getQueueStateColor } from "../utils/colors"

import classes from "./JobQueueIndexPage.module.css"

export const JobQueueIndexPage = () => {

  const [page, setPage] = useState(1)
  // TODO paginate
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
          <PaginatedTable
            page={page}
            onPageChanged={setPage}
            totalItemCount={queuesQuery.data?.length ?? 0}
            tableHead={
              <TableRow>
                <TableCell style={{ width: 0 }}></TableCell>
                <TableCell>Queue ID</TableCell>
                <TableCell style={{ width: 0 }}>State</TableCell>
                <TableCell style={{ width: 0 }}># Scheduled</TableCell>
                <TableCell style={{ width: 0 }}># Pending</TableCell>
                <TableCell style={{ width: 0 }}># In Progress</TableCell>
                <TableCell style={{ width: 0 }}># Failed</TableCell>
              </TableRow>
            }
            tableBody={
              <>
                {queuesQuery.isLoading && <TableRowSkeleton numColumns={7} />}
                {queuesQuery.data?.map(it => (
                  <QueueTableRow key={it.queueId} queue={it} />
                ))}
              </>
            }
            className={classes.Table}
          />
        </TableContainer>
      </Paper>
    </Page>
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