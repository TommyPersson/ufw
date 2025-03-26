import GppGoodOutlinedIcon from "@mui/icons-material/GppGoodOutlined"
import GppMaybeOutlinedIcon from "@mui/icons-material/GppMaybeOutlined"
import { Chip, Paper, TableCell, TableContainer, TableRow, Tooltip, Typography } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import Markdown from "react-markdown"
import {
  LinkTableCell,
  Page,
  PageBreadcrumb,
  PaginatedTable,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import { DurableCacheItem } from "../../models"
import { DurableCachesListQuery } from "../../queries"

import classes from "./DurableCachesIndexPage.module.css"

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
          className={classes.CacheTable}
          totalItemCount={totalItemCount}
          page={page}
          onPageChanged={setPage}
          tableHead={
            <TableRow>
              <TableCell style={{ width: 150, whiteSpace: "nowrap", textAlign: "center" }}># Entries</TableCell>
              <TableCell>Cache</TableCell>
              <TableCell>Settings</TableCell>
              <TableCell></TableCell>
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
      <LinkTableCell to={link}>
        <PropertyGroup>
          <PropertyText
            title={"Expiration Time (Database)"}
            subtitle={cache.expirationDuration?.toHuman()}
          />
          <PropertyText
            title={"Expiration Time (In-Memory)"}
            subtitle={cache.inMemoryExpirationDuration?.toHuman()}
          />
        </PropertyGroup>
      </LinkTableCell>
      <LinkTableCell to={link}>
        {cache.containsSensitiveData ? (
          <Tooltip title={"It is not possible to view the full entries of this cache"}>
            <Chip
              color={"error"}
              label={"Contains Sensitive Data"}
              variant={"outlined"}
              icon={<GppMaybeOutlinedIcon />}
            />
          </Tooltip>
        ) : (
          <Tooltip title={"It is possible to view the full entries of this cache"}>
            <Chip
              color={"success"}
              label={"No Sensitive Data"}
              variant={"outlined"}
              icon={<GppGoodOutlinedIcon />}
            />
          </Tooltip>
        )}
      </LinkTableCell>
    </TableRow>
  )
}
