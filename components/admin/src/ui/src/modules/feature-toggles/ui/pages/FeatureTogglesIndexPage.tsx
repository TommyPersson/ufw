import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline"
import HighlightOffIcon from "@mui/icons-material/HighlightOff"
import { Chip, ChipProps, Paper, TableCell, TableContainer, TableRow, Typography } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import Markdown from "react-markdown"
import {
  CommandButton,
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
              <TableCell>State</TableCell>
              <TableCell>Feature Toggle</TableCell>
              <TableCell>Created At</TableCell>
              <TableCell>Last Changed At</TableCell>
              <TableCell></TableCell>
            </TableRow>
          }
          tableBody={
            <>
              {featureTogglesQuery.isLoading && <TableRowSkeleton numColumns={5} />}
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

  const commandArgs = { featureToggleId: featureToggle.id }

  return (
    <TableRow hover>
      <TableCell>
        <FeatureToggleStateChip featureToggle={featureToggle} />
      </TableCell>
      <TableCell>
        <Typography variant={"subtitle2"}>{featureToggle.title}</Typography>
        <Typography variant={"body2"} component={"div"}>
          <Markdown>{featureToggle.description}</Markdown>
        </Typography>
        <Typography variant={"caption"}>ID: <code>{featureToggle.id}</code></Typography>
      </TableCell>
      <TableCell><DateTimeText dateTime={featureToggle.createdAt} /></TableCell>
      <TableCell><DateTimeText dateTime={featureToggle.stateChangedAt} /></TableCell>
      <TableCell>
        {!featureToggle.isEnabled &&
            <CommandButton
                command={EnableFeatureToggleCommand}
                args={commandArgs}
                variant={"contained"}
                size={"small"}
                disabled={isFetching}
            />
        }
        {featureToggle.isEnabled &&
            <CommandButton
                command={DisableFeatureToggleCommand}
                args={commandArgs}
                variant={"contained"}
                size={"small"}
                disabled={isFetching}
            />
        }
      </TableCell>
    </TableRow>
  )
}

const emptyTableRow = <TableRow><TableCell colSpan={3}>
  <center><em>No feature toggles found</em></center>
</TableCell></TableRow>

const FeatureToggleStateChip = (props: { featureToggle: FeatureToggleItem }) => {
  const { featureToggle } = props

  const color: ChipProps["color"] = featureToggle.isEnabled ? "success" : "error"
  const label = featureToggle.isEnabled ? "Enabled" : "Disabled"
  const icon = featureToggle.isEnabled ? <CheckCircleOutlineIcon /> : <HighlightOffIcon />

  return (
    <Chip
      variant={"outlined"}
      icon={icon}
      color={color}
      label={label}
    />
  )
}