import { Table, TableBody, TableFooter, TableHead, TablePagination, TableProps, TableRow } from "@mui/material"
import { useCallback } from "react"

export type PaginatedTableProps = {
  totalItemCount: number,
  page: number,
  onPageChanged: (page: number) => void
  tableHead?: any
  tableBody?: any
  tableFooter?: any
  className?: string
  tableProps?: TableProps
}
export const PaginatedTable = (props: PaginatedTableProps) => {
  const { totalItemCount, page, onPageChanged, tableHead, tableBody, tableFooter, className } = props

  const handlePageChanged = useCallback((_: any, page: number) => {
    onPageChanged(page + 1)
  }, [onPageChanged])

  return (
    <Table className={className} {...props.tableProps}>
      <TableHead>
        <TableRow>
          <TablePagination
            count={totalItemCount}
            onPageChange={handlePageChanged}
            page={page - 1}
            rowsPerPage={100}
            rowsPerPageOptions={[]}
          />
        </TableRow>
        {tableHead}
      </TableHead>
      <TableBody>
        {tableBody}
      </TableBody>
      <TableFooter>
        <TableRow>
          {tableFooter}
          <TablePagination
            count={totalItemCount}
            onPageChange={handlePageChanged}
            page={page - 1}
            rowsPerPage={100}
            rowsPerPageOptions={[]}
          />
        </TableRow>
      </TableFooter>
    </Table>
  )
}