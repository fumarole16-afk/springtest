import { useState } from 'react';
import { compareStocks } from '../api/stockApi';
import type { CompareData } from '../api/stockApi';
import TickerInput from '../components/TickerInput';
import CompareChart from '../components/CompareChart';
import RadarComparison from '../components/RadarComparison';

function formatMarketCap(cap: number): string {
  if (!cap) return '-';
  if (cap >= 1e12) return `$${(cap / 1e12).toFixed(2)}T`;
  if (cap >= 1e9) return `$${(cap / 1e9).toFixed(2)}B`;
  if (cap >= 1e6) return `$${(cap / 1e6).toFixed(2)}M`;
  return `$${cap}`;
}

const PERIODS = [
  { label: '1개월', value: '1m' },
  { label: '3개월', value: '3m' },
  { label: '1년', value: '1y' },
];

export default function Compare() {
  const [tickers, setTickers] = useState<string[]>([]);
  const [period, setPeriod] = useState('1m');
  const [data, setData] = useState<CompareData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  function handleCompare() {
    if (tickers.length < 2) return;
    setLoading(true);
    setError('');
    compareStocks(tickers, period)
      .then(setData)
      .catch(() => setError('비교 데이터를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }

  return (
    <div>
      <h2 className="text-2xl font-bold mb-4">종목 비교</h2>

      <TickerInput tickers={tickers} onChange={setTickers} onCompare={handleCompare} />

      <div className="flex gap-2 mb-4">
        {PERIODS.map((p) => (
          <button
            key={p.value}
            onClick={() => setPeriod(p.value)}
            className={`px-3 py-1 rounded text-sm ${period === p.value ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-300 hover:bg-slate-600'}`}
          >
            {p.label}
          </button>
        ))}
      </div>

      {loading && <div className="text-slate-400">로딩 중...</div>}
      {error && <div className="text-red-400">{error}</div>}

      {data && !loading && (
        <>
          {/* Price return chart */}
          <CompareChart prices={data.prices} tickers={tickers} />

          {/* Basic metrics table */}
          <div className="bg-slate-800 rounded-lg p-4 mb-6 overflow-x-auto">
            <h3 className="text-sm font-semibold text-slate-300 mb-3">기본 지표 비교</h3>
            <table className="w-full text-sm">
              <thead>
                <tr className="text-slate-400 text-left">
                  <th className="pb-2 pr-4">티커</th>
                  <th className="pb-2 pr-4">회사명</th>
                  <th className="pb-2 pr-4">현재가</th>
                  <th className="pb-2 pr-4">시가총액</th>
                  <th className="pb-2 pr-4">PER</th>
                  <th className="pb-2 pr-4">섹터</th>
                </tr>
              </thead>
              <tbody>
                {data.stocks.map((s) => (
                  <tr key={s.ticker} className="border-t border-slate-700">
                    <td className="py-2 pr-4 font-medium text-blue-400">{s.ticker}</td>
                    <td className="py-2 pr-4">{s.companyName}</td>
                    <td className="py-2 pr-4">${s.currentPrice?.toFixed(2) || '-'}</td>
                    <td className="py-2 pr-4">{formatMarketCap(s.marketCap)}</td>
                    <td className="py-2 pr-4">{s.trailingPE?.toFixed(2) || '-'}</td>
                    <td className="py-2 pr-4">{s.sectorName || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Radar comparison */}
          <RadarComparison financials={data.financials} tickers={tickers} />

          {/* Financial comparison table */}
          <div className="bg-slate-800 rounded-lg p-4 mb-6 overflow-x-auto">
            <h3 className="text-sm font-semibold text-slate-300 mb-3">재무제표 비교 (최근 연간)</h3>
            <table className="w-full text-sm">
              <thead>
                <tr className="text-slate-400 text-left">
                  <th className="pb-2 pr-4">티커</th>
                  <th className="pb-2 pr-4">기간</th>
                  <th className="pb-2 pr-4">매출</th>
                  <th className="pb-2 pr-4">영업이익</th>
                  <th className="pb-2 pr-4">순이익</th>
                </tr>
              </thead>
              <tbody>
                {tickers.flatMap((t) => {
                  const rows = data.financials[t] || [];
                  return rows.slice(-3).map((f, i) => (
                    <tr key={`${t}-${i}`} className="border-t border-slate-700">
                      <td className="py-2 pr-4 font-medium text-blue-400">{i === 0 ? t : ''}</td>
                      <td className="py-2 pr-4">{f.period}</td>
                      <td className="py-2 pr-4">{f.revenue ? `$${(f.revenue / 1e9).toFixed(1)}B` : '-'}</td>
                      <td className="py-2 pr-4">{f.operatingIncome ? `$${(f.operatingIncome / 1e9).toFixed(1)}B` : '-'}</td>
                      <td className="py-2 pr-4">{f.netIncome ? `$${(f.netIncome / 1e9).toFixed(1)}B` : '-'}</td>
                    </tr>
                  ));
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
