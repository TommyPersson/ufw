import { TableCell, TableCellProps } from "@mui/material"
import classNames from "classnames"
import { Link } from "react-router"

import classes from "./LinkTableCell.module.css"

export const LinkTableCell = (props: { to: string } & TableCellProps) => {
  const { to, children, ...tableCellProps } = props

  const className = classNames(classes.LinkTableCell, props.className)

  return (
    <TableCell {...tableCellProps} className={className}>
      <Link to={to}>
        <div style={{ background: "" }}>
          {children}
        </div>
      </Link>
    </TableCell>
  )
}