import WarningIcon from "@mui/icons-material/Warning"
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow
} from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"
import { useParams } from "react-router"
import { DateTimeText, Page, PageBreadcrumb } from "../../../../common/components"
import { JobListItem } from "../../models/JobListItem"
import { JobQueueDetails } from "../../models/JobQueueDetails"
import { JobState } from "../../models/JobState"
import { JobListQuery } from "../../queries/JobListQuery"
import { JobQueueDetailsQuery } from "../../queries/JobQueueDetailsQuery"

export const JobListPage = () => {
  const params = useParams<{ queueId: string, jobState: JobState }>()
  const queueId = params.queueId!
  const jobState = params.jobState!

  // TODO pagination

  const queueDetailsQuery = useQuery(JobQueueDetailsQuery(queueId))
  const jobListQuery = useQuery(JobListQuery(queueId, jobState, 1))

  const handleRefresh = () => {
    jobListQuery.refetch().then()
    queueDetailsQuery.refetch().then()
  }

  const isLoading = queueDetailsQuery.isFetching || jobListQuery.isFetching

  const totalItemCount = getTotalItemCountForState(queueDetailsQuery.data ?? null, jobState)

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Job Queue" },
    { text: "Queues", link: "../" },
    { text: <code>{queueId}</code>, link: `../queues/${queueId}/details` },
    { text: "Jobs" },
    { text: <code>{jobState}</code>, current: true },
  ], [queueId, jobState])

  return (
    <Page
      heading={<>Jobs</>}
      isLoading={isLoading}
      onRefresh={handleRefresh}
      breadcrumbs={breadcrumbs}
    >
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell></TableCell>
              <TableCell>ID</TableCell>
              <TableCell>State Changed At</TableCell>
              <TableCell>Next Scheduled For</TableCell>
              <TableCell># Failures</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {jobListQuery.data?.map(it =>
              <JobTableRow key={it.jobId} job={it} />
            )}
          </TableBody>
            <TablePagination
              count={totalItemCount}
              onPageChange={() => {
              }}
              page={0}
              rowsPerPage={100}
              rowsPerPageOptions={[]}
            />
        </Table>
      </TableContainer>
    </Page>
  )
}

const JobTableRow = (props: { job: JobListItem }) => {
  const { job } = props
  let hasFailures = job.numFailures > 0
  return (
    <TableRow key={job.jobId}>
      <TableCell>
        <Box width={24} height={24}>
          <WarningIcon
            color={"warning"}
            titleAccess={"The job has failures"}
            style={{ visibility: hasFailures ? "visible" : "hidden" }}
          />
        </Box>
      </TableCell>
      <TableCell>
        <code>{job.jobId}</code>
      </TableCell>
      <TableCell>
        <DateTimeText dateTime={job.stateChangedAt} />
      </TableCell>
      <TableCell>
        <DateTimeText dateTime={job.nextScheduledFor} fallback={<em>Not Currently Scheduled</em>} />
      </TableCell>
      <TableCell>
        {job.numFailures}
      </TableCell>
    </TableRow>
  )
}

function getTotalItemCountForState(queueDetails: JobQueueDetails | null, jobState: JobState) {
  if (!queueDetails) {
    return 0
  }

  switch (jobState) {
    case "SCHEDULED":
      return queueDetails.numScheduled
    case "PENDING":
      return queueDetails.numPending
    case "IN_PROGRESS":
      return queueDetails.numInProgress
    case "FAILED":
      return queueDetails.numFailed
    default:
      return 0
  }
}