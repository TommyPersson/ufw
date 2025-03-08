import { Box, Toolbar } from "@mui/material"
import { Outlet } from "react-router"

import classes from "./PageContainer.module.css"

export const PageContainer = () => {
  return (
    <Box
      className={classes.PageContainer}
      component={"main"}

    >
      <Toolbar />
      <Outlet />
    </Box>
  )
}