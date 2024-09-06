import * as React from "react";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TablePagination from "@mui/material/TablePagination";
import TableRow from "@mui/material/TableRow";
import styles from "./Table.module.css";

export default function StickyHeadTable({ columns, rows }) {
  const [page, setPage] = React.useState(0);
  const [rowsPerPage, setRowsPerPage] = React.useState(50);

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(+event.target.value);
    setPage(0);
  };

  return (
    <>
      <TableContainer sx={{ maxHeight: 530 }}>
        <Table stickyHeader aria-label="sticky table">
          <TableHead className={styles.TableHead}>
            <TableRow className={styles.TableRow}>
              {columns.map((column, columnIndex) => (
                <TableCell
                  className={styles.TableCell}
                  key={`header-cell-${column.id ?? columnIndex}`}
                  align="left"
                  style={{ minWidth: column.minWidth }}
                >
                  {column.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows
              .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
              .map((row, rowIndex) => {
                const rowId = row.id ?? rowIndex;
                return (
                  <TableRow
                    className={styles.TableRow}
                    hover
                    role="checkbox"
                    tabIndex={-1}
                    key={`row-${rowId}`}
                  >
                    {columns.map((column, columnIndex) => {
                      const value = row[column.id];
                      const cellId = `${rowId}-${column.id ?? columnIndex}`;
                      return (
                        <TableCell
                          className={styles.TableCell}
                          key={`cell-${cellId}`}
                          align="left"
                        >
                          {column.format && typeof value === "number"
                            ? column.format(value)
                            : value}
                        </TableCell>
                      );
                    })}
                  </TableRow>
                );
              })}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        rowsPerPageOptions={[50, 100, 150, 200]}
        component="div"
        count={rows.length}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={handleChangePage}
        onRowsPerPageChange={handleChangeRowsPerPage}
        sx={{
          "& .MuiTablePagination-toolbar, & .MuiTablePagination-selectLabel, & .MuiTablePagination-input, & .MuiTablePagination-displayedRows, & .MuiTablePagination-menuItem, & .MuiTablePagination-actions":
            {
              fontSize: "1.2rem",
              fontFamily: "var(--font-family)",
              fontWeight: 300,
              color: "var(--heading-and-text-color)",
            },
        }}
      />
    </>
  );
}
