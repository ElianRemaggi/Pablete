import DashboardIcon from '@mui/icons-material/Dashboard';
import PersonIcon from '@mui/icons-material/Person';
import HistoryEduIcon from '@mui/icons-material/HistoryEdu';
import AssignmentIcon from '@mui/icons-material/Assignment';
import SchoolIcon from '@mui/icons-material/School';
import WorkspacePremiumIcon from '@mui/icons-material/WorkspacePremium';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import { DashboardLayout, type NavItem } from '../components/DashboardLayout';

const navItems: NavItem[] = [
  { label: 'Dashboard', to: '/alumno/dashboard', icon: <DashboardIcon /> },
  { label: 'Perfil', to: '/alumno/perfil', icon: <PersonIcon /> },
  { label: 'Calendario', to: '/alumno/calendario', icon: <CalendarMonthIcon /> },
  { label: 'Historial académico', to: '/alumno/historial-academico', icon: <HistoryEduIcon /> },
  { label: 'Inscripción a cursada', to: '/alumno/inscripcion-cursada', icon: <AssignmentIcon /> },
  { label: 'Inscripción a final', to: '/alumno/inscripcion-final', icon: <SchoolIcon /> },
  { label: 'Certificados', to: '/alumno/certificados', icon: <WorkspacePremiumIcon /> },
];

export function AlumnoLayout() {
  return <DashboardLayout titulo="Panel del Alumno" navItems={navItems} />;
}
