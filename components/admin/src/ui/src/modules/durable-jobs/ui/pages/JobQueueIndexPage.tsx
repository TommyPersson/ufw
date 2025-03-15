import WarningIcon from "@mui/icons-material/Warning"
import { Box, Paper, Skeleton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { Link } from "react-router"
import { Page, PageBreadcrumb } from "../../../../common/components"
import { JobQueueListQuery } from "../../queries"

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
      <Paper>
        <TableContainer>
          <Table className={classes.Table}>
            <TableHead>
              <TableRow>
                <TableCell></TableCell>
                <TableCell>Queue ID</TableCell>
                <TableCell># Scheduled</TableCell>
                <TableCell># Pending</TableCell>
                <TableCell># In Progress</TableCell>
                <TableCell># Failed</TableCell>
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
                </TableRow>
              )}
              {queuesQuery.data?.map(it => {
                const hasFailures = it.numFailed > 0

                return (
                  <TableRow hover key={it.queueId}>
                    <TableCell>
                      <Box width={24} height={24}>
                        <WarningIcon
                          color={"warning"}
                          titleAccess={"The queue contains failed jobs"}
                          style={{ visibility: hasFailures ? "visible" : "hidden" }}
                        />
                      </Box>
                    </TableCell>
                    <TableCell><code><Link to={`queues/${it.queueId}/details`}>{it.queueId}</Link></code></TableCell>
                    <TableCell>{it.numScheduled}</TableCell>
                    <TableCell>{it.numPending}</TableCell>
                    <TableCell>{it.numInProgress}</TableCell>
                    <TableCell>{it.numFailed}</TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Page>
  )

}