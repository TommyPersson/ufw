import GppGoodOutlinedIcon from "@mui/icons-material/GppGoodOutlined"
import GppMaybeOutlinedIcon from "@mui/icons-material/GppMaybeOutlined"
import {
  Card,
  CardContent,
  Chip, Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography
} from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { uniqBy } from "es-toolkit"
import { useMemo } from "react"
import Markdown from "react-markdown"
import {
  ApplicationModuleHeader,
  LinkTableCell,
  Page,
  PageBreadcrumb,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import { ApplicationModule } from "../../../../common/models"
import { DurableCacheItem } from "../../models"
import { DurableCachesListQuery } from "../../queries"

import classes from "./DurableCachesIndexPage.module.css"

export const DurableCachesIndexPage = () => {

  const durableCachesListQuery = useQuery(DurableCachesListQuery(1))
  const caches = durableCachesListQuery.data?.items ?? []

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Durable Caches" },
    { text: "All", current: true },
  ], [])

  const applicationModules = uniqBy(caches.map(it => it.applicationModule), it => it.id)

  return (
    <Page
      heading={"Caches"}
      breadcrumbs={breadcrumbs}
      isLoading={durableCachesListQuery.isFetching}
      onRefresh={durableCachesListQuery.refetch}
      autoRefresh={true}
    >
      {applicationModules.map(module => {
        const cachesInModule = caches.filter(cache => cache.applicationModule.id === module.id)
        return <DurableCacheTableCard
          key={module.id}
          module={module}
          caches={cachesInModule}
        />
      })}
    </Page>
  )
}

const DurableCacheTableCard = (props: {
  caches: DurableCacheItem[]
  module: ApplicationModule
}) => {
  const { caches, module } = props

  return (
    <TableContainer component={Card}>
      <CardContent>
        <ApplicationModuleHeader applicationModule={module} />
      </CardContent>
      <Divider />
      <Table className={classes.CacheTable}>
        <TableHead>
          <TableRow>
            <TableCell style={{ width: 150, whiteSpace: "nowrap", textAlign: "center" }}># Entries</TableCell>
            <TableCell>Cache</TableCell>
            <TableCell>Settings</TableCell>
            <TableCell></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          <>
            {caches.map(it =>
              <DurableCacheItemRow key={it.id} cache={it} />
            )}
          </>
        </TableBody>
      </Table>
    </TableContainer>
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
        <Typography variant={"caption"}>ID: <code>{cache.id}</code></Typography>
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
