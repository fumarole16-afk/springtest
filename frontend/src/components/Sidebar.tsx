import { NavLink } from 'react-router-dom';
import StockSearch from './StockSearch';

const navItems = [
  { to: '/', label: '대시보드', icon: '📊' },
  { to: '/screening', label: '스크리닝', icon: '🔍' },
  { to: '/sectors', label: '섹터 분석', icon: '📈' },
  { to: '/compare', label: '종목 비교', icon: '⚖️' },
  { to: '/news', label: '뉴스', icon: '📰' },
];

export default function Sidebar() {
  return (
    <aside className="w-60 bg-slate-800 text-white flex flex-col h-screen fixed left-0 top-0">
      <div className="p-4 border-b border-slate-700">
        <h1 className="text-lg font-bold mb-3">Stock Analyzer</h1>
        <StockSearch />
      </div>
      <nav className="flex-1 p-2">
        {navItems.map((item) => (
          <NavLink key={item.to} to={item.to}
            className={({ isActive }) =>
              `flex items-center gap-2 px-3 py-2 rounded-lg text-sm mb-1 ${isActive ? 'bg-slate-700 text-white' : 'text-slate-300 hover:bg-slate-700'}`
            }>
            <span>{item.icon}</span><span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
