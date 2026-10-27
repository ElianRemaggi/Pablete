import DashboardIcon from '@mui/icons-material/Dashboard';
import SearchIcon from '@mui/icons-material/Search';
import GradeIcon from '@mui/icons-material/Grade';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import EventNoteIcon from '@mui/icons-material/EventNote';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import { DashboardLayout, type NavItem } from '../components/DashboardLayout';

const navItems: NavItem[] = [
  { label: 'Inicio', to: '/preceptor/dashboard', icon: <DashboardIcon /> },
  { label: 'Acreditación de cursadas', to: '/preceptor/acreditacion-cursadas', icon: <GradeIcon /> },
  { label: 'Mesas y notas de finales', to: '/preceptor/mesas-examen', icon: <EventNoteIcon /> },
  { label: 'Búsqueda de alumnos', to: '/preceptor/busqueda-alumnos', icon: <SearchIcon /> },
  { label: 'Importar historial', to: '/preceptor/importar-historial', icon: <UploadFileIcon /> },
  { label: 'Calendario', to: '/preceptor/calendario', icon: <CalendarMonthIcon /> },
  { label: 'Exportaciones', to: '/preceptor/exportaciones', icon: <FileDownloadIcon /> },
];

export function PreceptorLayout() {
  return <DashboardLayout titulo="Panel del Preceptor" navItems={navItems} />;
}
