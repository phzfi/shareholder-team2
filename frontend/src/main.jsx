import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import ErrorPage from "./error-page";
import Root, { loader as rootLoader } from "./routes/root";
import Osakasluettelo, {
  loader as osakasluetteloLoader,
} from "./routes/osakasluettelo";
import Osakenumerot, {
  loader as osakenumerotLoader,
} from "./routes/osakenumerot";
import Merkintahistoria from "./routes/merkintahistoria";
import Osakkaidentiedot from "./routes/osakkaidentiedot";
import LisaaUusi from "./routes/lisaa-uusi";
import "./reset.css";
import "./index.css";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Root />,
    errorElement: <ErrorPage />,
    loader: rootLoader,
    children: [
      {
        path: "osakasluettelo",
        element: <Osakasluettelo />,
        loader: osakasluetteloLoader,
      },
      {
        path: "osakenumerot",
        element: <Osakenumerot />,
        loader: osakenumerotLoader,
      },
      {
        path: "merkintahistoria",
        element: <Merkintahistoria />,
      },
      {
        path: "osakkaidentiedot",
        element: <Osakkaidentiedot />,
      },
      {
        path: "lisaa-uusi",
        element: <LisaaUusi />,
      },
    ],
  },
]);

createRoot(document.getElementById("root")).render(
  <RouterProvider router={router} />
);
