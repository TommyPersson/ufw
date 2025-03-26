import ArticleOutlinedIcon from "@mui/icons-material/ArticleOutlined"
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined"
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  InputAdornment,
  Skeleton,
  TableCell,
  TableRow,
  TextField,
  Typography
} from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useDebounce } from "@uidotdev/usehooks"
import { useCallback, useEffect, useMemo, useState } from "react"
import Markdown from "react-markdown"
import { useParams } from "react-router"
import {
  CommandButton,
  DateTimeText,
  ErrorAlert,
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
import { booleanToYesNo } from "../../../../common/utils/translations"
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
        isLoading={isLoading}
      />

      <ActionsSection
        cacheId={cacheId}
      />

      <CacheEntriesSection
        cacheId={cacheId}
        cacheDetails={cacheDetails}
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
        error={cacheEntryDetailsQuery.error}
        isLoading={cacheEntryDetailsQuery.isLoading}
        onClose={handleDetailsModalClose}
      />
    </Page>
  )
}

const DetailsSection = (props: {
  cacheDetails: DurableCacheDetails | null,
  isLoading: boolean
}) => {
  const { cacheDetails, isLoading } = props

  return (
    <PageSectionCard heading={cacheDetails?.title ?? <Skeleton />}>
      <Typography variant={"body2"} component={"div"}>
        <Markdown>{cacheDetails?.description}</Markdown>
      </Typography>
      <PropertyGroup horizontal>
        <PropertyGroup>
          <PropertyText
            title={"ID"}
            subtitle={<code>{cacheDetails?.id}</code>}
            isLoading={isLoading}
          />
          <PropertyText
            title={"# Entries"}
            subtitle={cacheDetails?.numEntries}
            isLoading={isLoading}
          />
        </PropertyGroup>
        <PropertyGroup>
          <PropertyText
            title={"Expiration Time (Database)"}
            subtitle={cacheDetails?.expirationDuration?.toHuman()}
            isLoading={isLoading}
          />
          <PropertyText
            title={"Expiration Time (In-Memory)"}
            subtitle={cacheDetails?.inMemoryExpirationDuration?.toHuman()}
            isLoading={isLoading}
          />
        </PropertyGroup>
        <PropertyGroup>
          <PropertyText
            title={"Contains Sensitive Data"}
            subtitle={booleanToYesNo(cacheDetails?.containsSensitiveData ?? true)}
            isLoading={isLoading}
          />
        </PropertyGroup>
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
  cacheDetails: DurableCacheDetails | null,
  entries: DurableCacheEntryItem[]
  isLoadingEntries: boolean
  keyPrefix: string
  onKeyPrefixChanged: (value: string) => void
  page: number,
  onPageChanged: (page: number) => void
  onEntrySelected: (cacheKey: string) => void
}) => {
  const { cacheId, cacheDetails, entries, isLoadingEntries, keyPrefix, onKeyPrefixChanged, page, onPageChanged } = props

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
              <CacheEntryTableRow
                key={it.key}
                cacheId={cacheId}
                cacheDetails={cacheDetails}
                entry={it}
                onEntrySelected={props.onEntrySelected}
              />
            ))}
          </>
        }
      />
    </PageSectionCard>
  )
}

const CacheEntryTableRow = (props: {
  cacheId: string
  cacheDetails: DurableCacheDetails | null,
  entry: DurableCacheEntryItem
  onEntrySelected: (cacheKey: string) => void
}) => {
  const { cacheId, cacheDetails, entry, onEntrySelected } = props

  return (
    <TableRow hover>
      <TableCell>
        <code>{entry.key}</code>
      </TableCell>
      <TableCell>
        <DateTimeText dateTime={entry.cachedAt} />
      </TableCell>
      <TableCell>
        <DateTimeText dateTime={entry.expiresAt} />
      </TableCell>
      <TableCell>
        {cacheDetails?.containsSensitiveData === false && (
          <Button
            onClick={() => onEntrySelected(entry.key)}
            startIcon={<ArticleOutlinedIcon />}
            color={"primary"}
            variant={"outlined"}
            size={"small"}
            sx={{ mr: 1 }}
            children={"View details"}
          />
        )}
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
  error: any | null
  isLoading: boolean
  onClose: () => void
}) => {
  const {
    cacheId,
    selectedEntryKey,
    cacheEntryDetails,
    error,
    isLoading,
    onClose
  } = props

  const isOpen = selectedEntryKey != null

  const content = error ? (
    <ErrorAlert error={error} />
  ) : isOpen ? (
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