import WarningIcon from "@mui/icons-material/Warning"
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableFooter,
  TableHead,
  TablePagination,
  TableRow
} from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import * as React from "react"
import { useCallback, useMemo, useState } from "react"
import { useParams } from "react-router"
import { DateTimeText, Page, PageBreadcrumb, TableRowSkeleton } from "../../../../common/components"
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

  const handlePageChanged = useCallback((_: React.MouseEvent<HTMLButtonElement> | null, newPage: number) => {
    setPage(newPage + 1)
  }, [setPage])

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
        <Table>
          <TableHead>
            <TableRow>
              <TableCell></TableCell>
              <TableCell>ID</TableCell>
              <TableCell>Created At</TableCell>
              <TableCell>State Changed At</TableCell>
              <TableCell>Next Scheduled For</TableCell>
              <TableCell># Failures</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading && <TableRowSkeleton numColumns={6} />}
            {isEmpty && emptyTableRow}
            {jobListQuery.data?.items?.map(it =>
              <JobTableRow key={it.jobId} job={it} />
            )}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TablePagination
                count={totalItemCount}
                onPageChange={handlePageChanged}
                page={page-1}
                rowsPerPage={100}
                rowsPerPageOptions={[]}
              />
            </TableRow>
          </TableFooter>
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
        <DateTimeText dateTime={job.createdAt} />
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

const emptyTableRow = <TableRow><TableCell colSpan={6}><center><em>No items found</em></center></TableCell></TableRow>