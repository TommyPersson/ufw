import { Skeleton, TableCell, TableRow } from "@mui/material"

export type TableRowSkeletonProps = {
  numColumns: number
}


export const TableRowSkeleton = (props: TableRowSkeletonProps) => {

  const { numColumns } = props

  const cells = range(numColumns).map(i => (
    <TableCell key={`skel-${i}`}>
      <Skeleton />
    </TableCell>
  ))

  return (
    <TableRow>
      {cells}
    </TableRow>
  )
}

function range(numColumns: number) {
  return new Array(numColumns).fill(0).map(i => i)
}