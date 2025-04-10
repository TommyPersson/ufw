import { Card, TableCell, TableRow } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { DateTimeText, Page, PaginatedTable } from "../../../../common/components"
import { PaginatedList } from "../../../../common/utils/api"
import { ClusterInstance } from "../../models"
import { ClusterInstancesListQuery } from "../../queries"


export const ClusterIndexPage = () => {

  const clusterInstancesQuery = useQuery(ClusterInstancesListQuery)
  const clusterInstances = clusterInstancesQuery.data ?? { items: [], hasMoreItems: false }

  return (
    <Page
      heading={"Cluster Instances"}
      error={clusterInstancesQuery.error}
      isLoading={clusterInstancesQuery.isFetching}
      onRefresh={clusterInstancesQuery.refetch}
      autoRefresh={true}
    >
      <ClusterTableCard instances={clusterInstances} />
    </Page>
  )
}

const ClusterTableCard = (props: { instances: PaginatedList<ClusterInstance> }) => {
  const { instances } = props

  return (
    <Card>
      <PaginatedTable
        totalItemCount={instances.hasMoreItems ? -1 : instances.items.length} // TODO full count
        page={1}
        pageSize={100}
        onPageChanged={() => {
        }}
        tableHead={
          <TableRow>
            <TableCell>Cluster Instance ID</TableCell>
            <TableCell>App Version</TableCell>
            <TableCell>Started At</TableCell>
            <TableCell>Last Heartbeat Time</TableCell>
          </TableRow>
        }
        tableBody={
          instances.items.map(it =>
            <ClusterInstanceRow key={it.instanceId} instance={it} />
          )
        }
      />
    </Card>
  )
}

const ClusterInstanceRow = (props: { instance: ClusterInstance }) => {
  const { instance } = props
  return (
    <TableRow hover>
      <TableCell><code>{instance.instanceId}</code></TableCell>
      <TableCell><code>{instance.appVersion}</code></TableCell>
      <TableCell><DateTimeText dateTime={instance.startedAt} /></TableCell>
      <TableCell><DateTimeText dateTime={instance.heartbeatTimestamp} /></TableCell>
    </TableRow>
  )
}
