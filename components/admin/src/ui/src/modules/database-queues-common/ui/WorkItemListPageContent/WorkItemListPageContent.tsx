import WarningIcon from "@mui/icons-material/Warning"
import { Box, Paper, TableCell, TableContainer, TableRow } from "@mui/material"
import { DateTimeText, LinkTableCell, PaginatedTable, TableRowSkeleton } from "../../../../common/components"
import { WorkItemListItem, WorkItemState, WorkQueueDetails } from "../../models"
import { DatabaseQueueAdapterSettings } from "../../DatabaseQueueAdapterSettings"


export const WorkItemListPageContent = (props: {
  queueId: string
  state: WorkItemState
  workQueueDetails: WorkQueueDetails | null
  workItems: WorkItemListItem[]
  isLoading: boolean
  page: number
  setPage: (page: number) => void
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const {
    queueId,
    state,
    workQueueDetails,
    workItems,
    isLoading,
    page,
    setPage,
    adapterSettings
  } = props

  const isEmpty = !isLoading && workItems.length === 0
  const totalItemCount = getTotalItemCountForState(workQueueDetails, state)

  return (
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
            {workItems.map(it =>
              <WorkItemTableRow key={it.itemId} queueId={queueId} item={it} adapterSettings={adapterSettings} />
            )}
          </>
        }
      />
    </TableContainer>
  )
}

const WorkItemTableRow = (props: {
  queueId: string
  item: WorkItemListItem
  adapterSettings: DatabaseQueueAdapterSettings
}) => {
  const { queueId, item, adapterSettings } = props

  const hasFailures = item.numFailures > 0
  const link = adapterSettings.linkFormatters.workItemDetails(queueId, item.itemId)
  const queueType = adapterSettings.texts.queueTypeSingular.toLowerCase()

  return (
    <TableRow key={item.itemId} hover>
      <LinkTableCell to={link}>
        <Box width={24} height={24}>
          <WarningIcon
            color={"warning"}
            titleAccess={`The ${queueType} has failures`}
            style={{ visibility: hasFailures ? "visible" : "hidden" }}
          />
        </Box>
      </LinkTableCell>
      <LinkTableCell to={link}>
        <code>{item.itemId}</code>
      </LinkTableCell>
      <LinkTableCell to={link}>
        <DateTimeText dateTime={item.createdAt} />
      </LinkTableCell>
      <LinkTableCell to={link}>
        <DateTimeText dateTime={item.stateChangedAt} />
      </LinkTableCell>
      <LinkTableCell to={link}>
        <DateTimeText dateTime={item.nextScheduledFor} fallback={<em>Not Currently Scheduled</em>} />
      </LinkTableCell>
      <LinkTableCell to={link}>
        {item.numFailures}
      </LinkTableCell>
    </TableRow>
  )
}

const emptyTableRow = <TableRow><TableCell colSpan={6}>
  <center><em>No items found</em></center>
</TableCell></TableRow>


function getTotalItemCountForState(queueDetails: WorkQueueDetails | null, state: WorkItemState) {
  if (!queueDetails) {
    return 0
  }

  switch (state) {
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
