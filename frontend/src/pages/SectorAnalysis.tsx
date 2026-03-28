import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getSectors } from '../api/stockApi';
import type { SectorOverview } from '../api/stockApi';
import SectorHeatmap from '../components/SectorHeatmap';
import PerformanceChart from '../components/PerformanceChart';

const PERIODS = ['1D', '1W', '1M', '3M', '1Y'];

function formatMarketCap(cap: number): string {
  if (cap >= 1e12) return `$${(cap / 1e12).toFixed(2)}T`;
  if (cap >= 1e9) return `$${(cap / 1e9).toFixed(2)}B`;
  if (cap >= 1e6) return `$${(cap / 1e6).toFixed(2)}M`;
  return `$${cap}`;
}

export default function SectorAnalysis() {
  const navigate = useNavigate();
  const [sectors, setSectors] = useState<SectorOverview[]>([]);
  const [loading, setLoading] = useState(true);
  const [period, setPeriod] = useState('1M');

  useEffect(() => {
    getSectors()
      .then(setSectors)
      .catch(() => setSectors([]))
      .finally(() => setLoading(false));
  }, []);

  const perfData = sectors.map((s) => ({
    name: s.sectorName,
    value: s.avgDividendYield ?? 0,
  }));

  if (loading) return <div className="text-slate-400">로딩 중...</div>;

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">섹터 분석</h2>

      <SectorHeatmap sectors={sectors} />

      <div className="mt-6">
        <div className="flex gap-2 mb-3">
          {PERIODS.map((p) => (
            <button key={p} onClick={() => setPeriod(p)}
              className={`px-3 py-1 rounded text-sm ${period === p ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-300 hover:bg-slate-600'}`}>
              {p}
            </button>
          ))}
        </div>
        <PerformanceChart data={perfData} period={period} />
      </div>

      <div className="mt-6 bg-slate-800 rounded-lg overflow-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-slate-400 border-b border-slate-700">
              <th className="px-4 py-3 text-left">섹터명</th>
              <th className="px-4 py-3 text-right">평균 PER</th>
              <th className="px-4 py-3 text-right">평균 PBR</th>
              <th className="px-4 py-3 text-right">시가총액</th>
              <th className="px-4 py-3 text-right">배당수익률</th>
            </tr>
          </thead>
          <tbody>
            {sectors.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-slate-400">데이터가 없습니다.</td>
              </tr>
            ) : sectors.map((s) => (
              <tr key={s.sectorId}
                className="border-b border-slate-700 hover:bg-slate-700 cursor-pointer"
                onClick={() => navigate(`/sectors/${s.sectorId}`)}>
                <td className="px-4 py-3 font-semibold text-blue-400">{s.sectorName}</td>
                <td className="px-4 py-3 text-right">{s.avgPer?.toFixed(2) ?? '-'}</td>
                <td className="px-4 py-3 text-right">{s.avgPbr?.toFixed(2) ?? '-'}</td>
                <td className="px-4 py-3 text-right">{s.totalMarketCap ? formatMarketCap(s.totalMarketCap) : '-'}</td>
                <td className="px-4 py-3 text-right">{s.avgDividendYield !== undefined ? `${s.avgDividendYield.toFixed(2)}%` : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
