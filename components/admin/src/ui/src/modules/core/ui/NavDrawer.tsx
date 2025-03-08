import { Divider, Drawer, List, ListItem, ListItemButton, ListItemText, ListSubheader, Toolbar } from "@mui/material"
import { NavLink, useMatch } from "react-router"
import { allModuleDefinitions } from "../../modules"
import { ApplicationMetadata } from "../models/ApplicationMetadata"

import classes from "./NavDrawer.module.css"

export const NavDrawer = (props: {
  applicationMetadata: ApplicationMetadata
}) => {
  const {
    applicationMetadata
  } = props

  const availableModuleIds = applicationMetadata.availableModuleIds

  const navItems = allModuleDefinitions
    .filter(it => availableModuleIds.includes(it.moduleId))
    .map(it => {
    return (
      <NavItem
        key={it.moduleId}
        link={it.indexRoute}
        title={it.navItemTitle}
      />
    )
  })

  return (
    <Drawer
      variant={"permanent"}
      sx={{
        width: 300,
        [`& .MuiDrawer-paper`]: { width: 300, boxSizing: "border-box" },
      }}
    >
      <Toolbar />
      <Divider />
      <List>
        <NavItem link={"/"} title={"Home"} />
      </List>
      <List subheader={<ListSubheader>Admin Modules</ListSubheader>}>
        {navItems}
      </List>
    </Drawer>
  )
}

const NavItem = (props: { link: string, title: string }) => {
  const { link, title } = props

  const isSelected = !!useMatch(`${link}/*`)

  return (
    <ListItem disablePadding className={classes.NavItem}>
      <NavLink to={link}>
        <ListItemButton selected={isSelected}>
          <ListItemText>
            {title}
          </ListItemText>
        </ListItemButton>
      </NavLink>
    </ListItem>
  )
}