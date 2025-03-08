import CssBaseline from "@mui/material/CssBaseline"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import { createBrowserRouter, RouterProvider } from "react-router"
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
    <QueryClientProvider client={new QueryClient()}>
      <CssBaseline />
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)
