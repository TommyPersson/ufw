import WarningIcon from "@mui/icons-material/Warning"
import { Box, Paper, TableCell, TableContainer, TableRow } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import { useParams } from "react-router"
import {
  DateTimeText,
  LinkTableCell,
  Page,
  PageBreadcrumb,
  PaginatedTable,
  TableRowSkeleton
} from "../../../../common/components"
import { JobListItem, JobQueueDetails, JobState } from "../../models"
import { JobListQuery, JobQueueDetailsQuery } from "../../queries"

export const JobListPage = () => {
  const params = useParams<{ queueId: string, jobState: JobState }>()
  const queueId = params.queueId!
  const jobState = params.jobState!

  const [page, setPage] = useState(1)

  const queueDetailsQuery = useQuery(JobQueueDetailsQuery(queueId))
  const jobListQuery = useQuery(JobListQuery(queueId, jobState, page))

  const handleRefresh = () => {
    jobListQuery.refetch().then()
    queueDetailsQuery.refetch().then()
  }

  const isLoading = queueDetailsQuery.isLoading || jobListQuery.isLoading
  const isFetching = queueDetailsQuery.isFetching || jobListQuery.isFetching

  const isEmpty = !isLoading && (jobListQuery.data?.items?.length ?? 0) === 0
  const totalItemCount = getTotalItemCountForState(queueDetailsQuery.data ?? null, jobState)

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Jobs" },
    { text: "Job Queues", link: "../" },
    { text: <code>{queueId}</code>, link: `../queues/${queueId}/details` },
    { text: "Jobs" },
    { text: <code>{jobState}</code>, current: true },
  ], [queueId, jobState])

  return (
    <Page
      heading={<>Jobs</>}
      isLoading={isFetching}
      onRefresh={handleRefresh}
      breadcrumbs={breadcrumbs}
    >
      <TableContainer component={Paper}>
        <PaginatedTable
          page={page}
          onPageChanged={setPage}
          totalItemCount={totalItemCount}
          tableHead={
            <TableRow>
              <TableCell></TableCell>
              <TableCell>ID</TableCell>
              <TableCell>Created At</TableCell>
              <TableCell>State Changed At</TableCell>
              <TableCell>Next Scheduled For</TableCell>
              <TableCell># Failures</TableCell>
            </TableRow>
          }
          tableBody={
            <>
              {isLoading && <TableRowSkeleton numColumns={6} />}
              {isEmpty && emptyTableRow}
              {jobListQuery.data?.items?.map(it =>
                <JobTableRow key={it.jobId} queueId={queueId} job={it} />
              )}
            </>
          }
        />
      </TableContainer>
    </Page>
  )
}

const JobTableRow = (props: { queueId: string, job: JobListItem }) => {
  const { queueId, job } = props

  const hasFailures = job.numFailures > 0
  const link = `/durable-jobs/queues/${queueId}/jobs/by-id/${job.jobId}/details`

  return (
    <TableRow key={job.jobId} hover>
      <LinkTableCell to={link}>
        <Box width={24} height={24}>
          <WarningIcon
            color={"warning"}
            titleAccess={"The job has failures"}
            style={{ visibility: hasFailures ? "visible" : "hidden" }}
          />
        </Box>
      </LinkTableCell>
      <LinkTableCell to={link}>
        <code>{job.jobId}</code>
      </LinkTableCell>
      <LinkTableCell to={link}>
        <DateTimeText dateTime={job.createdAt} />
      </LinkTableCell>
      <LinkTableCell to={link}>
        <DateTimeText dateTime={job.stateChangedAt} />
      </LinkTableCell>
      <LinkTableCell to={link}>
        <DateTimeText dateTime={job.nextScheduledFor} fallback={<em>Not Currently Scheduled</em>} />
      </LinkTableCell>
      <LinkTableCell to={link}>
        {job.numFailures}
      </LinkTableCell>
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

const emptyTableRow = <TableRow><TableCell colSpan={6}>
  <center><em>No items found</em></center>
</TableCell></TableRow>