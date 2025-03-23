import { Paper, TableCell, TableContainer, TableRow, Typography } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import Markdown from "react-markdown"
import { LinkTableCell, Page, PageBreadcrumb, PaginatedTable } from "../../../../common/components"
import { DurableCacheItem } from "../../models"
import { DurableCachesListQuery } from "../../queries"


export const DurableCachesIndexPage = () => {

  const [page, setPage] = useState(1)

  const durableCachesListQuery = useQuery(DurableCachesListQuery(page))
  const caches = durableCachesListQuery.data?.items ?? []
  const totalItemCount = durableCachesListQuery.data?.items?.length ?? 0

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Caches" },
    { text: "Caches", current: true },
  ], [])

  return (
    <Page
      heading={"Caches"}
      breadcrumbs={breadcrumbs}
      isLoading={durableCachesListQuery.isFetching}
      onRefresh={durableCachesListQuery.refetch}
      autoRefresh={true}
    >
      <TableContainer component={Paper}>
        <PaginatedTable
          totalItemCount={totalItemCount}
          page={page}
          onPageChanged={setPage}
          tableHead={
            <TableRow>
              <TableCell style={{ width: 0, whiteSpace: "nowrap" }}># Entries</TableCell>
              <TableCell>Cache</TableCell>
            </TableRow>
          }
          tableBody={
            <>
              {caches.map(it =>
                <DurableCacheItemRow key={it.id} cache={it} />
              )}
            </>
          }
        />
      </TableContainer>
    </Page>
  )
}

const DurableCacheItemRow = (props: { cache: DurableCacheItem }) => {
  const { cache } = props

  const link = `caches/${cache.id}`

  return (
    <TableRow hover>
      <LinkTableCell to={link} style={{ textAlign: "center" }}>
        {cache.numEntries}
      </LinkTableCell>
      <LinkTableCell to={link}>
        <Typography variant={"subtitle2"}>{cache.title}</Typography>
        <Markdown>{cache.description}</Markdown>
        <code>{cache.id}</code>
      </LinkTableCell>
    </TableRow>
  )
}
