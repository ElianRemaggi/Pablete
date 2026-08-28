import DashboardIcon from '@mui/icons-material/Dashboard';
import GroupIcon from '@mui/icons-material/Group';
import SearchIcon from '@mui/icons-material/Search';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import LibraryBooksIcon from '@mui/icons-material/LibraryBooks';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import ScheduleIcon from '@mui/icons-material/Schedule';
import ClassIcon from '@mui/icons-material/Class';
import EventNoteIcon from '@mui/icons-material/EventNote';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import SettingsIcon from '@mui/icons-material/Settings';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import BarChartIcon from '@mui/icons-material/BarChart';
import { DashboardLayout, type NavItem } from '../components/DashboardLayout';

const navItems: NavItem[] = [
  { label: 'Dashboard', to: '/administrador/dashboard', icon: <DashboardIcon /> },
  { label: 'Usuarios', to: '/administrador/usuarios', icon: <GroupIcon /> },
  { label: 'Búsqueda de alumnos', to: '/administrador/busqueda-alumnos', icon: <SearchIcon /> },
  { label: 'Calendario', to: '/administrador/calendario', icon: <CalendarMonthIcon /> },
  { label: 'Carreras', to: '/administrador/carreras', icon: <AccountBalanceIcon /> },
  { label: 'Planes de estudio', to: '/administrador/planes-estudio', icon: <MenuBookIcon /> },
  { label: 'Materias', to: '/administrador/materias', icon: <LibraryBooksIcon /> },
  { label: 'Correlatividades', to: '/administrador/correlatividades', icon: <AccountTreeIcon /> },
  { label: 'Horarios', to: '/administrador/horarios', icon: <ScheduleIcon /> },
  { label: 'Comisiones', to: '/administrador/comisiones', icon: <ClassIcon /> },
  { label: 'Mesas de examen', to: '/administrador/mesas-examen', icon: <EventNoteIcon /> },
  { label: 'Configuración', to: '/administrador/configuracion', icon: <SettingsIcon /> },
  { label: 'Auditoría', to: '/administrador/auditoria', icon: <FactCheckIcon /> },
  { label: 'Reportes', to: '/administrador/reportes', icon: <BarChartIcon /> },
];

export function AdministradorLayout() {
  return <DashboardLayout titulo="Panel del Administrador" navItems={navItems} />;
}
