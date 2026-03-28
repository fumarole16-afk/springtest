import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import StockDetail from './pages/StockDetail';
import Screening from './pages/Screening';
import SectorAnalysis from './pages/SectorAnalysis';
import SectorDetail from './pages/SectorDetail';
import IndustryDetail from './pages/IndustryDetail';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/stocks/:ticker" element={<StockDetail />} />
          <Route path="/screening" element={<Screening />} />
          <Route path="/sectors" element={<SectorAnalysis />} />
          <Route path="/sectors/:id" element={<SectorDetail />} />
          <Route path="/industries/:id" element={<IndustryDetail />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
