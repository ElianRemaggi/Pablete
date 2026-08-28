import { createBrowserRouter } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { ForbiddenPage } from './pages/ForbiddenPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { RequireRole } from './auth/RequireRole';
import { AlumnoLayout } from './layouts/AlumnoLayout';
import { PreceptorLayout } from './layouts/PreceptorLayout';
import { AdministradorLayout } from './layouts/AdministradorLayout';
import { AlumnoDashboardPage } from './pages/alumno/DashboardPage';
import { AlumnoPerfilPage } from './pages/alumno/PerfilPage';
import { HistorialAcademicoPage } from './pages/alumno/HistorialAcademicoPage';
import { AlumnoInscripcionCursadaPage } from './pages/alumno/InscripcionCursadaPage';
import { AlumnoInscripcionFinalPage } from './pages/alumno/InscripcionFinalPage';
import { CertificadosPage } from './pages/alumno/CertificadosPage';
import { AlumnoCalendarioPage } from './pages/alumno/CalendarioPage';
import { CarrerasPage } from './pages/administrador/CarrerasPage';
import { MateriasPage } from './pages/administrador/MateriasPage';
import { CorrelatividadesPage } from './pages/administrador/CorrelatividadesPage';
import { UsuariosPage } from './pages/administrador/UsuariosPage';
import { ConfiguracionPage } from './pages/administrador/ConfiguracionPage';
import { AuditoriaPage } from './pages/administrador/AuditoriaPage';
import { MesasExamenPage } from './pages/administrador/MesasExamenPage';
import { PlanesEstudioPage } from './pages/administrador/PlanesEstudioPage';
import { ComisionesPage } from './pages/administrador/ComisionesPage';
import { ReportesPage } from './pages/administrador/ReportesPage';
import { AdministradorDashboardPage } from './pages/administrador/DashboardPage';
import { PreceptorDashboardPage } from './pages/preceptor/DashboardPage';
import { BusquedaAlumnosPage } from './pages/preceptor/BusquedaAlumnosPage';
import { FichaAlumnoPage } from './pages/preceptor/FichaAlumnoPage';
import { ExportacionesPage } from './pages/preceptor/ExportacionesPage';
import { PreceptorCalendarioPage } from './pages/preceptor/CalendarioPage';
import { AdministradorCalendarioPage } from './pages/administrador/CalendarioPage';
import { StubPage } from './components/StubPage';
import { RootRedirect } from './RootRedirect';

export const router = createBrowserRouter([
  { path: '/', element: <RootRedirect /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/403', element: <ForbiddenPage /> },

  {
    element: <RequireRole rol="ALUMNO" />,
    children: [
      {
        element: <AlumnoLayout />,
        children: [
          { path: '/alumno/dashboard', element: <AlumnoDashboardPage /> },
          { path: '/alumno/perfil', element: <AlumnoPerfilPage /> },
          { path: '/alumno/calendario', element: <AlumnoCalendarioPage /> },
          { path: '/alumno/historial-academico', element: <HistorialAcademicoPage /> },
          { path: '/alumno/inscripcion-cursada', element: <AlumnoInscripcionCursadaPage /> },
          { path: '/alumno/inscripcion-final', element: <AlumnoInscripcionFinalPage /> },
          { path: '/alumno/certificados', element: <CertificadosPage /> },
        ],
      },
    ],
  },

  {
    element: <RequireRole rol="PRECEPTOR" />,
    children: [
      {
        element: <PreceptorLayout />,
        children: [
          { path: '/preceptor/dashboard', element: <PreceptorDashboardPage /> },
          { path: '/preceptor/busqueda-alumnos', element: <BusquedaAlumnosPage /> },
          { path: '/preceptor/alumno/:id', element: <FichaAlumnoPage /> },
          { path: '/preceptor/calendario', element: <PreceptorCalendarioPage /> },
          { path: '/preceptor/exportaciones', element: <ExportacionesPage /> },
        ],
      },
    ],
  },

  {
    element: <RequireRole rol="ADMINISTRADOR" />,
    children: [
      {
        element: <AdministradorLayout />,
        children: [
          { path: '/administrador/dashboard', element: <AdministradorDashboardPage /> },
          { path: '/administrador/usuarios', element: <UsuariosPage /> },
          { path: '/administrador/busqueda-alumnos', element: <BusquedaAlumnosPage /> },
          { path: '/administrador/alumno/:id', element: <FichaAlumnoPage /> },
          { path: '/administrador/calendario', element: <AdministradorCalendarioPage /> },
          { path: '/administrador/carreras', element: <CarrerasPage /> },
          { path: '/administrador/planes-estudio', element: <PlanesEstudioPage /> },
          { path: '/administrador/materias', element: <MateriasPage /> },
          { path: '/administrador/correlatividades', element: <CorrelatividadesPage /> },
          {
            path: '/administrador/horarios',
            element: (
              <StubPage titulo="Horarios" detalle="Los horarios se gestionan dentro de cada Comisión." />
            ),
          },
          { path: '/administrador/comisiones', element: <ComisionesPage /> },
          { path: '/administrador/mesas-examen', element: <MesasExamenPage /> },
          { path: '/administrador/configuracion', element: <ConfiguracionPage /> },
          { path: '/administrador/auditoria', element: <AuditoriaPage /> },
          { path: '/administrador/reportes', element: <ReportesPage /> },
        ],
      },
    ],
  },

  { path: '*', element: <NotFoundPage /> },
]);
