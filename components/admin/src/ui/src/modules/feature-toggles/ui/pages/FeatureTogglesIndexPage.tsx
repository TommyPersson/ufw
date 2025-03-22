import { Paper, TableCell, TableContainer, TableRow } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import {
  CommandSwitch,
  DateTimeText,
  Page,
  PageBreadcrumb,
  PaginatedTable,
  TableRowSkeleton
} from "../../../../common/components"
import { DisableFeatureToggleCommand, EnableFeatureToggleCommand } from "../../commands"
import { FeatureToggleItem } from "../../models"
import { FeatureToggleListQuery } from "../../queries"

import classes from "./FeatureTogglesIndexPage.module.css"

export const FeatureTogglesIndexPage = () => {

  const [page, setPage] = useState(1)

  const featureTogglesQuery = useQuery(FeatureToggleListQuery(page))
  const featureToggles = featureTogglesQuery.data?.items ?? []

  const totalItemCount = featureTogglesQuery.data?.items?.length ?? 0

  const isEmpty = !featureTogglesQuery.isLoading && featureToggles.length === 0

  const breadcrumbs = useMemo<PageBreadcrumb[]>(() => [
    { text: "Feature Toggles" },
    { text: "All", current: true },
  ], [])

  return (
    <Page
      heading={"Feature Toggles"}
      breadcrumbs={breadcrumbs}
      isLoading={featureTogglesQuery.isFetching}
      onRefresh={featureTogglesQuery.refetch}
      autoRefresh={true}
      error={featureTogglesQuery.error}
    >
      <TableContainer component={Paper}>
        <PaginatedTable
          className={classes.FeatureTogglesTable}
          totalItemCount={totalItemCount}
          page={page}
          onPageChanged={setPage}
          tableHead={
            <TableRow>
              <TableCell></TableCell>
              <TableCell>Feature Toggle ID</TableCell>
              <TableCell>Last Changed At</TableCell>
            </TableRow>
          }
          tableBody={
            <>
              {featureTogglesQuery.isLoading && <TableRowSkeleton numColumns={3} />}
              {isEmpty && emptyTableRow}
              {featureToggles.map(it => (
                <FeatureToggleRow
                  key={it.id}
                  featureToggle={it}
                  isFetching={featureTogglesQuery.isFetching}
                />
              ))}
            </>
          }
        />
      </TableContainer>
    </Page>
  )
}

const FeatureToggleRow = (props: {
  featureToggle: FeatureToggleItem,
  isFetching: boolean,
}) => {
  const { featureToggle, isFetching } = props

  return (
    <TableRow>
      <TableCell>
        <CommandSwitch
          enabled={featureToggle.isEnabled}
          enableCommand={EnableFeatureToggleCommand}
          disableCommand={DisableFeatureToggleCommand}
          args={{ featureToggleId: featureToggle.id }}
          disabled={isFetching}
        />
      </TableCell>
      <TableCell><code>{featureToggle.id}</code></TableCell>
      <TableCell><DateTimeText dateTime={featureToggle.stateChangedAt} /></TableCell>
    </TableRow>
  )
}

const emptyTableRow = <TableRow><TableCell colSpan={3}><center><em>No feature toggles found</em></center></TableCell></TableRow>