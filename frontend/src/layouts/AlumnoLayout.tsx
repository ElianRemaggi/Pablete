import DashboardIcon from '@mui/icons-material/Dashboard';
import PersonIcon from '@mui/icons-material/Person';
import HistoryEduIcon from '@mui/icons-material/HistoryEdu';
import AssignmentIcon from '@mui/icons-material/Assignment';
import SchoolIcon from '@mui/icons-material/School';
import ListAltIcon from '@mui/icons-material/ListAlt';
import WorkspacePremiumIcon from '@mui/icons-material/WorkspacePremium';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import { DashboardLayout, type NavItem } from '../components/DashboardLayout';
import { CURSADAS_HABILITADAS } from '../config/features';

const navItems: NavItem[] = [
  { label: 'Inicio', to: '/alumno/dashboard', icon: <DashboardIcon /> },
  { label: 'Datos personales', to: '/alumno/perfil', icon: <PersonIcon /> },
  { label: 'Historial académico', to: '/alumno/historial-academico', icon: <HistoryEduIcon /> },
  { label: 'Exámenes finales', to: '/alumno/inscripcion-final', icon: <SchoolIcon /> },
  { label: 'Mis inscripciones', to: '/alumno/mis-inscripciones', icon: <ListAltIcon /> },
  { label: 'Comprobantes y certificados', to: '/alumno/certificados', icon: <WorkspacePremiumIcon /> },
  { label: 'Calendario', to: '/alumno/calendario', icon: <CalendarMonthIcon /> },
  ...(CURSADAS_HABILITADAS
    ? [{ label: 'Inscripción a cursada', to: '/alumno/inscripcion-cursada', icon: <AssignmentIcon /> }]
    : []),
];

export function AlumnoLayout() {
  return <DashboardLayout titulo="Panel del Alumno" navItems={navItems} />;
}
