import CssBaseline from "@mui/material/CssBaseline"
import { QueryClientProvider } from "@tanstack/react-query"
import { ReactQueryDevtools } from "@tanstack/react-query-devtools"
import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import { createBrowserRouter, RouterProvider } from "react-router"
import { ConfirmProvider } from "./common/components"
import { queryClient } from "./common/utils/tsq"
import { routes } from "./routes"

import "@fontsource/roboto/300.css"
import "@fontsource/roboto/400.css"
import "@fontsource/roboto/500.css"
import "@fontsource/roboto/700.css"

import "./index.css"

const router = createBrowserRouter(routes, {
  basename: "/admin/ui",
})

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <CssBaseline />
      <ReactQueryDevtools />
      <ConfirmProvider>
        <RouterProvider router={router} />
      </ConfirmProvider>
    </QueryClientProvider>
  </StrictMode>,
)
