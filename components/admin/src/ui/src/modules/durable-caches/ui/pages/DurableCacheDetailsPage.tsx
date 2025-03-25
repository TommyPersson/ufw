import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle, InputAdornment,
  Skeleton,
  TableCell,
  TableRow,
  TextField,
  Typography
} from "@mui/material"
import ArticleOutlinedIcon from "@mui/icons-material/ArticleOutlined"
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined"
import { useQuery } from "@tanstack/react-query"
import { useDebounce } from "@uidotdev/usehooks"
import { useCallback, useEffect, useMemo, useState } from "react"
import Markdown from "react-markdown"
import { useParams } from "react-router"
import {
  CommandButton,
  DateTimeText,
  JsonBlock,
  Page,
  PageBreadcrumb,
  PageSectionCard,
  PageSectionHeader,
  PaginatedTable,
  PropertyGroup,
  PropertyText,
  TableRowSkeleton
} from "../../../../common/components"
import { InvalidateAllCacheEntriesCommand, InvalidateCacheEntryCommand } from "../../commands"
import { DurableCacheDetails, DurableCacheEntryItem } from "../../models"
import { DurableCacheEntryDetails } from "../../models/DurableCacheEntryDetails"
import { DurableCacheDetailsQuery, DurableCacheEntriesListQuery } from "../../queries"
import { DurableCacheEntryDetailsQuery } from "../../queries/DurableCacheEntryDetailsQuery"

import classes from "./DurableCacheDetailsPage.module.css"


export const DurableCacheDetailsPage = () => {
  const params = useParams<{ cacheId: string }>()
  const cacheId = params.cacheId!!

  const [keyPrefix, setKeyPrefix] = useState("")
  const [page, setPage] = useState(1)
  const [selectedEntryKey, setSelectedEntryKey] = useState<string | null>(null)

  const debouncedKeyPrefix = useDebounce(keyPrefix, 300)

  const cacheDetailsQuery = useQuery(DurableCacheDetailsQuery(cacheId))
  const cacheEntriesQuery = useQuery(DurableCacheEntriesListQuery(cacheId, debouncedKeyPrefix, page))
  const cacheEntryDetailsQuery = useQuery(DurableCacheEntryDetailsQuery(cacheId, selectedEntryKey ?? ""))

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

  const handleDetailsModalClose = useCallback(() => {
    setSelectedEntryKey(null)
  }, [setSelectedEntryKey])

  useEffect(() => {
    setSelectedEntryKey(null)
  }, [page, keyPrefix, cacheId])

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
        onEntrySelected={setSelectedEntryKey}
      />

      <CacheEntryDetailsDialog
        cacheId={cacheId}
        selectedEntryKey={selectedEntryKey}
        cacheEntryDetails={cacheEntryDetailsQuery.data ?? null}
        isLoading={cacheEntryDetailsQuery.isLoading}
        onClose={handleDetailsModalClose}
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
      <Typography variant={"body2"} component={"div"}>
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
  onEntrySelected: (cacheKey: string) => void
}) => {
  const { cacheId, entries, isLoadingEntries, keyPrefix, onKeyPrefixChanged, page, onPageChanged } = props

  const isEmpty = !isLoadingEntries && entries.length === 0

  return (
    <PageSectionCard heading={"Cache Entries"}>
      <TextField
        label={"Key Prefix Search"}
        value={keyPrefix}
        style={{ width: 400 }}
        onChange={(e) => onKeyPrefixChanged(e.target.value)}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position={"start"} children={<SearchOutlinedIcon />} />
            )
          }
        }}
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
              <CacheEntryTableRow key={it.key} cacheId={cacheId} entry={it} onEntrySelected={props.onEntrySelected} />
            ))}
          </>
        }
      />
    </PageSectionCard>
  )
}

const CacheEntryTableRow = (props: {
  cacheId: string
  entry: DurableCacheEntryItem
  onEntrySelected: (cacheKey: string) => void
}) => {
  const { cacheId, entry, onEntrySelected } = props
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
        <Button
          onClick={() => onEntrySelected(entry.key)}
          startIcon={<ArticleOutlinedIcon />}
          color={"primary"}
          variant={"outlined"}
          size={"small"}
          sx={{ mr: 1 }}
          children={"View details"}
        />
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
  const message = props.keyPrefix.length > 0
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

const CacheEntryDetailsDialog = (props: {
  cacheId: string
  selectedEntryKey: string | null,
  cacheEntryDetails: DurableCacheEntryDetails | null
  isLoading: boolean
  onClose: () => void
}) => {
  const {
    cacheId,
    selectedEntryKey,
    cacheEntryDetails,
    isLoading,
    onClose
  } = props

  const isOpen = selectedEntryKey != null

  const content = isOpen ? (
    <PropertyGroup>
      <PropertyGroup horizontal>
        <PropertyGroup>
          <PropertyText
            title={"Cache Key"}
            subtitle={<code>{selectedEntryKey}</code>}
            isLoading={isLoading}
          />
          <PropertyText
            title={"Value Type"}
            subtitle={<code>{cacheEntryDetails?.contentType}</code>}
            isLoading={isLoading}
          />
        </PropertyGroup>
        <PropertyGroup>
          <PropertyText
            title={"Cached At"}
            subtitle={<DateTimeText dateTime={cacheEntryDetails?.cachedAt ?? null} />}
            isLoading={isLoading}
          />
          <PropertyText
            title={"Expires At"}
            subtitle={<DateTimeText dateTime={cacheEntryDetails?.expiresAt ?? null} fallback={<em>Never</em>} />}
            isLoading={isLoading}
          />
        </PropertyGroup>
      </PropertyGroup>

      <PropertyText
        title={"Value"}
        subtitle={(
          <JsonBlock
            maxHeight={400}
            json={cacheEntryDetails?.content ?? ""}
          />
        )}
        isLoading={isLoading}
      />
    </PropertyGroup>
  ) : <Skeleton />

  return (
    <Dialog
      open={isOpen}
      closeAfterTransition={true}
      onClose={onClose}
      fullWidth={true}
    >
      <DialogTitle>Cache Entry Details</DialogTitle>
      <DialogContent>
        {content}
      </DialogContent>
      <DialogActions>
        <Button
          onClick={onClose}
          children={"Close"}
        />
        <CommandButton
          command={InvalidateCacheEntryCommand}
          args={{ cacheId, cacheKey: selectedEntryKey! }}
          onSuccess={onClose}
          variant={"contained"}
        />
      </DialogActions>
    </Dialog>
  )
}