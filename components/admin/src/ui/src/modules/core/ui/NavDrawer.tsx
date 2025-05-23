import HomeIcon from "@mui/icons-material/Home"
import {
  Divider,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Toolbar
} from "@mui/material"
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
    .flatMap(it => it.navItems)
    .map(it => {
      return (
        <NavItem
          key={it.route}
          link={it.route}
          title={it.title}
          icon={it.icon}
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
        <NavItem link={"/"} title={"Home"} icon={<HomeIcon />} />
      </List>
      <List subheader={<ListSubheader>Admin Modules</ListSubheader>}>
        {navItems}
      </List>
    </Drawer>
  )
}

const NavItem = (props: {
  link: string
  title: string
  icon?: any
}) => {
  const { link, title, icon } = props

  const isSelected = !!useMatch(`${link}/*`)

  return (
    <ListItem disablePadding className={classes.NavItem}>
      <NavLink to={link}>
        <ListItemButton selected={isSelected}>
          <ListItemIcon>
            {icon}
          </ListItemIcon>
          <ListItemText>
            {title}
          </ListItemText>
        </ListItemButton>
      </NavLink>
    </ListItem>
  )
}