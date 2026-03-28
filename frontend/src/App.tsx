import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import StockDetail from './pages/StockDetail';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/stocks/:ticker" element={<StockDetail />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
