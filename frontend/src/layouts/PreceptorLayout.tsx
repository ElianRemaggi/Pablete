import DashboardIcon from '@mui/icons-material/Dashboard';
import SearchIcon from '@mui/icons-material/Search';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import { DashboardLayout, type NavItem } from '../components/DashboardLayout';

const navItems: NavItem[] = [
  { label: 'Dashboard', to: '/preceptor/dashboard', icon: <DashboardIcon /> },
  { label: 'Búsqueda de alumnos', to: '/preceptor/busqueda-alumnos', icon: <SearchIcon /> },
  { label: 'Calendario', to: '/preceptor/calendario', icon: <CalendarMonthIcon /> },
  { label: 'Exportaciones', to: '/preceptor/exportaciones', icon: <FileDownloadIcon /> },
];

export function PreceptorLayout() {
  return <DashboardLayout titulo="Panel del Preceptor" navItems={navItems} />;
}
