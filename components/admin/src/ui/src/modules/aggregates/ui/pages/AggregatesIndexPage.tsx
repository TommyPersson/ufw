import { Skeleton, TablePagination, TextField, Typography } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useDebounce } from "@uidotdev/usehooks"
import { ChangeEvent, useCallback, useEffect, useMemo, useState } from "react"
import { useSearchParams } from "react-router"
import {
  DateTimeText,
  JsonBlock,
  Page,
  PageBreadcrumb,
  PageSectionCard,
  PageSectionHeader,
  PropertyGroup,
  PropertyText
} from "../../../../common/components"
import { AggregateDetails, AggregateFact } from "../../models"
import { AggregateDetailsQuery } from "../../queries"
import { AggregateFactsQuery } from "../../queries/AggregateFactsQuery"

export const AggregatesIndexPage = () => {

  const [searchParams, setSearchParams] = useSearchParams()

  const [page, setPage] = useState(1)

  const aggregateId = searchParams.get("id") ?? ""
  const debouncedAggregateId = useDebounce(aggregateId, 300)

  const aggregateDetailsQuery = useQuery(AggregateDetailsQuery(debouncedAggregateId))
  const aggregateDetails = aggregateDetailsQuery.data ?? null

  const aggregateFactsQuery = useQuery(AggregateFactsQuery(debouncedAggregateId, page))
  const aggregateFacts = aggregateFactsQuery.data?.items ?? []

  const isLoading = aggregateDetailsQuery.isLoading || aggregateFactsQuery.isLoading
  const isRefreshing = aggregateDetailsQuery.isFetching || aggregateFactsQuery.isFetching
  const error = aggregateDetailsQuery.error || aggregateFactsQuery.error

  const handleRefresh = () => {
    aggregateDetailsQuery.refetch().then()
    aggregateFactsQuery.refetch().then()
  }

  const handleAggregateSearchValueChanged = useCallback((value: string) => {
    setSearchParams({ id: value })
  }, [setSearchParams])

  useEffect(() => {
    setPage(1)
  }, [aggregateDetails])

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Aggregates" },
    { text: "Search", current: true },
  ], [])


  return (
    <Page
      heading={"Aggregates"}
      breadcrumbs={breadcrumbs}
      isLoading={isRefreshing}
      onRefresh={handleRefresh}
      error={error}
    >
      <AggregateSearchSection
        value={aggregateId}
        onChange={handleAggregateSearchValueChanged}
      />
      {aggregateDetails && (
        <>
          <AggregateDetailsSection
            details={aggregateDetails}
            isLoading={isLoading}
          />
          <AggregateFactsSection
            facts={aggregateFacts}
            isLoading={isLoading}
            hasMorePages={aggregateFactsQuery.data?.hasMoreItems ?? false}
            page={page}
            onPageChange={setPage}
          />
        </>
      )}
    </Page>
  )
}

const AggregateSearchSection = (props: {
  value: string,
  onChange: (value: string) => void
}) => {
  const { value, onChange } = props

  const handleFieldChange = useCallback((e: ChangeEvent<HTMLInputElement>) => {
    onChange(e.target.value)
  }, [onChange])

  return (
    <PageSectionCard heading={"Search"}>
      <TextField
        label={"Aggregate ID"}
        value={value}
        onChange={handleFieldChange}
      />
    </PageSectionCard>
  )
}

const AggregateDetailsSection = (props: {
  details: AggregateDetails | null
  isLoading: boolean
}) => {
  const { details, isLoading } = props

  const factTypes = details?.factTypes ?? []

  return (
    <>
      <PageSectionCard heading={"Details"}>
        <PropertyGroup horizontal>
          <PropertyGroup>
            <PropertyText
              title={"ID"}
              subtitle={<code>{details?.id}</code>}
              isLoading={isLoading}
            />
            <PropertyText
              title={"Type"}
              subtitle={<code>{details?.type}</code>}
              isLoading={isLoading}
            />
            <PropertyText
              title={"Version"}
              subtitle={details?.version}
              isLoading={isLoading}
            />
          </PropertyGroup>
          <PropertyGroup>
            <PropertyText
              title={"Available Fact Types"}
              subtitle={factTypes.map(it => (<><code key={it.type}>{it.type}</code><br /></>))}
              isLoading={isLoading}
            />
          </PropertyGroup>
        </PropertyGroup>
      </PageSectionCard>
      <PageSectionCard heading={"JSON Representation"}>
        {!details?.json && <Skeleton variant={"text"} />}
        {!details?.json && <Skeleton variant={"text"} />}
        {!details?.json && <Skeleton variant={"text"} />}
        {details?.json && <>
            <JsonBlock json={details.json} />
            <Typography variant={"caption"}>
                <strong>Note</strong>: This JSON representation is created by directly serializing the aggregate object.
                It may not reflect the complete state of the aggregate object.
            </Typography>
        </>}
      </PageSectionCard>
    </>
  )
}

const AggregateFactsSection = (props: {
  facts: AggregateFact[]
  page: number
  onPageChange: (page: number) => void
  hasMorePages: boolean
  isLoading: boolean
}) => {
  const { facts, /*isLoading,*/ page, onPageChange, hasMorePages } = props

  return (
    <>
      <PageSectionHeader>Facts</PageSectionHeader>
      {facts.map(fact => {
        return (
          <PageSectionCard key={fact.id}>
            <PropertyGroup horizontal boxProps={{ display: "grid", gridTemplateColumns: "1fr 2fr 3fr", gap: 1 }}>
              <PropertyGroup>
                <PropertyText
                  title={"Version"}
                  subtitle={<code>{fact.version}</code>}
                />
                <PropertyText
                  title={"Type"}
                  subtitle={<code>{fact.type}</code>}
                />
              </PropertyGroup>
              <PropertyGroup>
                <PropertyText
                  title={"Timestamp"}
                  subtitle={<DateTimeText dateTime={fact.timestamp} />}
                />
                <PropertyText
                  title={"ID"}
                  subtitle={<code>{fact.id}</code>}
                />
              </PropertyGroup>

              <PropertyText
                title={"JSON"}
                subtitle={<JsonBlock json={fact.json} maxHeight={200} />}
              />
            </PropertyGroup>
          </PageSectionCard>
        )
      })}
      <TablePagination
        component={"div"}
        page={page - 1}
        count={hasMorePages ? -1 : facts.length}
        rowsPerPage={100}
        rowsPerPageOptions={[]}
        onPageChange={(_, page) => onPageChange(page + 1)}
      />
    </>
  )
}
