import { Skeleton, TableCell, TableRow, TextField, Typography } from "@mui/material"
import { useDebounce } from "@uidotdev/usehooks"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import Markdown from "react-markdown"
import { useParams } from "react-router"
import {
  CommandButton,
  DateTimeText,
  Page,
  PageBreadcrumb,
  PageSectionCard, PageSectionHeader,
  PaginatedTable,
  PropertyGroup,
  PropertyText, TableRowSkeleton
} from "../../../../common/components"
import { InvalidateAllCacheEntriesCommand, InvalidateCacheEntryCommand } from "../../commands"
import { DurableCacheDetails, DurableCacheEntryItem } from "../../models"
import { DurableCacheDetailsQuery, DurableCacheEntriesListQuery } from "../../queries"

import classes from "./DurableCacheDetailsPage.module.css"

export const DurableCacheDetailsPage = () => {
  const params = useParams<{ cacheId: string }>()
  const cacheId = params.cacheId!!

  const [keyPrefix, setKeyPrefix] = useState("")
  const [page, setPage] = useState(1)

  const debouncedKeyPrefix = useDebounce(keyPrefix, 300)

  const cacheDetailsQuery = useQuery(DurableCacheDetailsQuery(cacheId))
  const cacheEntriesQuery = useQuery(DurableCacheEntriesListQuery(cacheId, debouncedKeyPrefix, page))

  const cacheDetails = cacheDetailsQuery.data ?? null
  const cacheEntries = cacheEntriesQuery.data?.items ?? []

  const isDebouncing = keyPrefix !== debouncedKeyPrefix
  const isLoading = cacheDetailsQuery.isLoading
  const isFetching = cacheDetailsQuery.isFetching || cacheEntriesQuery.isFetching
  const isLoadingEntries = isDebouncing || cacheEntriesQuery.isLoading

  const handleRefresh = () => {
    cacheDetailsQuery.refetch().then()
    cacheEntriesQuery.refetch().then()
  }

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Caches" },
    { text: "Caches", link: "../" },
    { text: <code>{cacheId}</code>, current: true },
  ], [cacheId])


  return (
    <Page
      heading={<>Cache Details</>}
      breadcrumbs={breadcrumbs}
      onRefresh={handleRefresh}
      isLoading={isFetching}
      autoRefresh={true}
    >
      <DetailsSection
        cacheDetails={cacheDetails}
        loading={isLoading}
      />

      <ActionsSection
        cacheId={cacheId}
      />

      <CacheEntriesSection
        cacheId={cacheId}
        entries={cacheEntries}
        isLoadingEntries={isLoadingEntries}
        keyPrefix={keyPrefix}
        onKeyPrefixChanged={setKeyPrefix}
        page={page}
        onPageChanged={setPage}
      />
    </Page>
  )
}

const DetailsSection = (props: {
  cacheDetails: DurableCacheDetails | null,
  loading: boolean
}) => {
  return (
    <PageSectionCard heading={props.cacheDetails?.title ?? <Skeleton />}>
      <Typography variant={"body2"}>
        <Markdown>{props.cacheDetails?.description}</Markdown>
      </Typography>
      <PropertyGroup horizontal>
        <PropertyText
          title={"ID"}
          subtitle={<code>{props.cacheDetails?.id}</code>}
          isLoading={props.loading}
        />
        <PropertyText
          title={"# Entries"}
          subtitle={props.cacheDetails?.numEntries}
          isLoading={props.loading}
        />
      </PropertyGroup>
    </PageSectionCard>
  )
}

const ActionsSection = (props: { cacheId: string }) => {
  return (
    <>
      <PageSectionHeader>Actions</PageSectionHeader>
      <CommandButton
        command={InvalidateAllCacheEntriesCommand}
        args={{ cacheId: props.cacheId }}
        style={{
          alignSelf: "flex-start"
        }}
        variant={"contained"}
      />
    </>
  )
}

const CacheEntriesSection = (props: {
  cacheId: string
  entries: DurableCacheEntryItem[]
  isLoadingEntries: boolean
  keyPrefix: string
  onKeyPrefixChanged: (value: string) => void
  page: number,
  onPageChanged: (page: number) => void
}) => {
  const { cacheId, entries, isLoadingEntries, keyPrefix, onKeyPrefixChanged, page, onPageChanged } = props

  const isEmpty = !isLoadingEntries && entries.length === 0

  return (
    <PageSectionCard heading={"Entries"}>
      <TextField
        label={"Key Prefix"}
        value={keyPrefix}
        onChange={(e) => onKeyPrefixChanged(e.target.value)}
      />
      <PaginatedTable
        totalItemCount={entries.length}
        page={page}
        onPageChanged={onPageChanged}
        className={classes.CacheEntriesTable}
        tableProps={{
          size: "small",
        }}
        tableHead={
          <TableRow>
            <TableCell>Cache Key</TableCell>
            <TableCell>Cached At</TableCell>
            <TableCell>Expires At</TableCell>
            <TableCell></TableCell>
          </TableRow>
        }
        tableBody={
          <>
            {isLoadingEntries && <TableRowSkeleton numColumns={4} />}
            {isEmpty && <EmptyTableRow keyPrefix={keyPrefix} />}
            {!isLoadingEntries && entries.map(it => (
              <CacheEntryTableRow key={it.key} cacheId={cacheId} entry={it} />
            ))}
          </>
        }
      />
    </PageSectionCard>
  )
}

const CacheEntryTableRow = (props: { cacheId: string, entry: DurableCacheEntryItem }) => {
  const { cacheId, entry } = props
  return (
    <TableRow hover>
      <TableCell>
        <code>{props.entry.key}</code>
      </TableCell>
      <TableCell>
        <DateTimeText dateTime={props.entry.cachedAt} />
      </TableCell>
      <TableCell>
        <DateTimeText dateTime={props.entry.expiresAt} />
      </TableCell>
      <TableCell>
        <CommandButton
          command={InvalidateCacheEntryCommand}
          args={{ cacheId: cacheId, cacheKey: entry.key }}
          size={"small"}
          variant={"outlined"}
        />
      </TableCell>
    </TableRow>
  )
}

const EmptyTableRow = (props: { keyPrefix: string }) => {
  let message = props.keyPrefix.length > 0
    ? <>No cache entries found with key prefix <code>{props.keyPrefix}</code></>
    : <>Please specify a key prefix to search for entries</>

  return (
    <TableRow>
      <TableCell colSpan={4}>
        <center><em>{message}</em></center>
      </TableCell>
    </TableRow>
  )
}