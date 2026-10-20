import DashboardIcon from '@mui/icons-material/Dashboard';
import SearchIcon from '@mui/icons-material/Search';
import EventNoteIcon from '@mui/icons-material/EventNote';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import { DashboardLayout, type NavItem } from '../components/DashboardLayout';

const navItems: NavItem[] = [
  { label: 'Inicio', to: '/preceptor/dashboard', icon: <DashboardIcon /> },
  { label: 'Mesas e inscriptos', to: '/preceptor/mesas-examen', icon: <EventNoteIcon /> },
  { label: 'Búsqueda de alumnos', to: '/preceptor/busqueda-alumnos', icon: <SearchIcon /> },
  { label: 'Calendario', to: '/preceptor/calendario', icon: <CalendarMonthIcon /> },
  { label: 'Exportaciones', to: '/preceptor/exportaciones', icon: <FileDownloadIcon /> },
];

export function PreceptorLayout() {
  return <DashboardLayout titulo="Panel del Preceptor" navItems={navItems} />;
}
