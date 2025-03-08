import { useQuery } from "@tanstack/react-query"
import { Page } from "../../../../common/components"
import { JobQueueListQuery } from "../../queries/JobQueueListQuery"
import { Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from "@mui/material"

export const JobQueueIndexPage = () => {
  const queuesQuery = useQuery(JobQueueListQuery)

  return (
    <Page heading={"Job Queues"}>
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Queue ID</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {queuesQuery.data?.map(it => (
                <TableRow>
                  <TableCell><code>{it.queueId}</code></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Page>
  )

}