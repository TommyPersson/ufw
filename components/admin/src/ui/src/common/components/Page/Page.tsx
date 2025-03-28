import RefreshIcon from "@mui/icons-material/Refresh"
import { Box, Breadcrumbs, Button, LinearProgress, Menu, MenuItem, Typography } from "@mui/material"
import classNames from "classnames"
import { useEffect, useState } from "react"
import { Link } from "react-router"
import { ErrorAlert } from "../ErrorAlert"
import { NavBarPortal } from "../NavBarPortal"

import classes from "./Page.module.css"

export type PageBreadcrumb = {
  text: any
  link?: string
  current?: boolean
}

export type PageProps = {
  heading?: any
  breadcrumbs?: PageBreadcrumb[]
  isLoading?: boolean
  autoRefresh?: boolean
  onRefresh?: () => void
  error?: Error | null
  fill?: boolean
  children: any
}

export const Page = (props: PageProps) => {
  const {
    heading,
    isLoading = false,
    onRefresh,
    autoRefresh = false,
    error,
    fill = false,
    children
  } = props

  const className = classNames(
    classes.Page,
    fill ? classes.Filled : classes.NonFilled
  )

  const breadcrumbs: PageBreadcrumb[] = [
    { text: "Home", link: '/' },
    ...(props.breadcrumbs ?? [])
  ]

  return (
    <Box className={className}>
      <PageHeader
        heading={heading}
        isLoading={isLoading}
        breadcrumbs={breadcrumbs}
      />
      <ErrorAlert error={error ?? null} />
      {children}
      <NavBarPortal>
        {onRefresh && <AutoRefreshControl
            isLoading={isLoading}
            autoRefresh={autoRefresh}
            onRefresh={onRefresh}
        />}
      </NavBarPortal>
    </Box>
  )
}

const PageHeader = (props: {
  heading: any,
  isLoading: boolean,
  breadcrumbs: PageBreadcrumb[]
}) => {
  const { heading, isLoading, breadcrumbs } = props
  const hasBreadcrumbs = breadcrumbs.length > 0
  return (
    <Box className={classes.PageHeader}>
      {hasBreadcrumbs && <PageBreadcrumbs breadcrumbs={breadcrumbs} />}
      {heading && <Typography variant={"h4"} component={"h2"} sx={{ mb: 2 }}>{heading}</Typography>}
      {isLoading && <div className={classes.PageLoadingIndicator}><LinearProgress /></div>}
    </Box>
  )
}

const PageBreadcrumbs = (props: { breadcrumbs: PageBreadcrumb[] }) => {
  return (
    <Breadcrumbs>
      {props.breadcrumbs.map(it => {
        let content
        if (it.link) {
          content = <Link to={it.link}>{it.text}</Link>
        } else {
          content = <>{it.text}</>
        }

        return <Typography key={it.text.toString()} sx={{ color: it.current ? "text.primary" : undefined }}>{content}</Typography>
      })}
    </Breadcrumbs>
  )
}

const AutoRefreshControl = (props: {
  isLoading: boolean,
  autoRefresh: boolean,
  onRefresh: () => void,
}) => {

  const { isLoading, autoRefresh, onRefresh } = props

  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>()
  const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false)

  const [refreshInterval, setRefreshInterval] = useState<number | null>(autoRefresh ? 15 : null)

  const handleMenuItemClicked = (value: number | null) => {
    setRefreshInterval(value)
    setIsMenuOpen(false)
  }

  useEffect(() => {
    if (!refreshInterval) {
      return
    }

    const handle = setInterval(onRefresh, refreshInterval! * 1_000)

    return () => {
      clearInterval(handle)
    }
  }, [autoRefresh, onRefresh, refreshInterval])

  return (
    <Box flexDirection={"row"}>
      <Button
        onClick={props.onRefresh}
        variant={"contained"}
        size={"large"}
        startIcon={<RefreshIcon />}
        loading={isLoading}
        children={"Refresh"}
      />
      <Button
        variant={"contained"}
        size={"large"}
        children={`Auto: ${formatIntervalOption(refreshInterval)}`}
        onClick={() => setIsMenuOpen(true)}
        ref={setMenuAnchor}
      />
      <Menu
        open={isMenuOpen}
        anchorEl={menuAnchor}
        onClose={() => setIsMenuOpen(false)}
      >
        {intervalOptions.map(it => {
          const text = formatIntervalOption(it)
          return (
            <MenuItem
              key={`interval-option-${it}`}
              selected={refreshInterval === it}
              disabled={refreshInterval === it}
              onClick={() => handleMenuItemClicked(it)}
              children={text}
            />
          )
        })}
      </Menu>
    </Box>
  )
}

const intervalOptions: (number | null)[] = [null, 5, 10, 15]

function formatIntervalOption(it: number | null) {
  return it ? `${it} s` : "Off"
}