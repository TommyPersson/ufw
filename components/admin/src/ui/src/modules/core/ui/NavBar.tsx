import HomeIcon from "@mui/icons-material/Home"
import { AppBar, Box, IconButton, Toolbar, Typography } from "@mui/material"
import { Link } from "react-router"
import { navBarPortalContainerId, PropertyText } from "../../../common/components"
import { ApplicationMetadata } from "../models/ApplicationMetadata"

import classes from "./NavBar.module.css"

export const NavBar = (props: {
  applicationMetadata: ApplicationMetadata
}) => {
  return (
    <AppBar
      position={"fixed"}
      sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}
      className={classes.NavBar}
    >
      <Toolbar className={classes.HeadingToolbar}>
        <Link to={"/"}>
          <IconButton
            size={"large"}
            edge={"start"}
            color={"inherit"}
            children={<HomeIcon />}
            sx={{ mr: 2 }}
          />
        </Link>
        <Typography
          variant={"h6"}
          component={"div"}
          children={"UFW Admin"}
          sx={{ mr: 2 }}
        />
      </Toolbar>
      <Toolbar className={classes.AppInfoToolbar}>
        <PropertyText
          title={"Application"}
          subtitle={props.applicationMetadata.name}
          sx={{ mr: 4 }}
        />
        <PropertyText
          title={"Version"}
          subtitle={props.applicationMetadata.version}
          sx={{ mr: 4 }}
        />
      </Toolbar>
      <Box flex={1} />
      <Toolbar>
        <div id={navBarPortalContainerId}></div>
      </Toolbar>
    </AppBar>
  )
}