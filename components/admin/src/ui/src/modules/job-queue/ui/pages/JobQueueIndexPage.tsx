import { useQuery } from "@tanstack/react-query"
import { Link } from "react-router"
import { Page } from "../../../../common/components"
import { JobQueueListQuery } from "../../queries/JobQueueListQuery"
import { Paper, Skeleton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from "@mui/material"
import WarningIcon from "@mui/icons-material/Warning"

import classes from "./JobQueueIndexPage.module.css"

export const JobQueueIndexPage = () => {
  const queuesQuery = useQuery(JobQueueListQuery)

  return (
    <Page
      heading={"Job Queues"}
      isLoading={queuesQuery.isFetching}
      onRefresh={queuesQuery.refetch}
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
              {queuesQuery.isLoading &&
                  <TableRow>
                      <TableCell><WarningIcon style={{ visibility: "hidden" }} /></TableCell>
                      <TableCell><Skeleton variant="text" /></TableCell>
                      <TableCell><Skeleton variant="text" /></TableCell>
                      <TableCell><Skeleton variant="text" /></TableCell>
                      <TableCell><Skeleton variant="text" /></TableCell>
                      <TableCell><Skeleton variant="text" /></TableCell>
                  </TableRow>
              }
              {queuesQuery.data?.map(it => {
                const hasFailures = it.numFailed > 0

                return (
                  <TableRow hover key={it.queueId}>
                    <TableCell>
                      <WarningIcon
                        color={"warning"}
                        titleAccess={"The queue contains failed jobs"}
                        style={{ visibility: hasFailures ? "visible" : "hidden" }}
                      />
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