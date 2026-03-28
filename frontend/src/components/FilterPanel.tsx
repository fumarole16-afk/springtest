import { useEffect, useState } from 'react';
import { getSectors } from '../api/stockApi';
import type { ScreeningFilter, SectorOverview } from '../api/stockApi';

interface Props {
  onFilterChange: (filter: ScreeningFilter) => void;
}

export default function FilterPanel({ onFilterChange }: Props) {
  const [sectors, setSectors] = useState<SectorOverview[]>([]);
  const [filter, setFilter] = useState<ScreeningFilter>({});

  useEffect(() => {
    getSectors().then(setSectors).catch(() => {});
  }, []);

  function setNum(key: keyof ScreeningFilter, value: string) {
    const parsed = value === '' ? undefined : Number(value);
    setFilter((prev) => ({ ...prev, [key]: parsed }));
  }

  function handleApply() {
    onFilterChange(filter);
  }

  return (
    <div className="bg-slate-800 rounded-lg p-4 w-72 flex-shrink-0">
      <h3 className="text-sm font-semibold text-slate-300 mb-4">필터</h3>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">섹터</label>
        <select
          className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
          value={filter.sectorId ?? ''}
          onChange={(e) => setFilter((prev) => ({ ...prev, sectorId: e.target.value === '' ? undefined : Number(e.target.value) }))}
        >
          <option value="">전체</option>
          {sectors.map((s) => (
            <option key={s.sectorId} value={s.sectorId}>{s.sectorName}</option>
          ))}
        </select>
      </div>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">거래소</label>
        <select
          className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
          value={filter.exchange ?? ''}
          onChange={(e) => setFilter((prev) => ({ ...prev, exchange: e.target.value || undefined }))}
        >
          <option value="">전체</option>
          <option value="NYSE">NYSE</option>
          <option value="NASDAQ">NASDAQ</option>
        </select>
      </div>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">PER 범위</label>
        <div className="flex gap-2">
          <input type="number" placeholder="최소" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
            value={filter.minPer ?? ''} onChange={(e) => setNum('minPer', e.target.value)} />
          <input type="number" placeholder="최대" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
            value={filter.maxPer ?? ''} onChange={(e) => setNum('maxPer', e.target.value)} />
        </div>
      </div>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">PBR 범위</label>
        <div className="flex gap-2">
          <input type="number" placeholder="최소" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
            value={filter.minPbr ?? ''} onChange={(e) => setNum('minPbr', e.target.value)} />
          <input type="number" placeholder="최대" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
            value={filter.maxPbr ?? ''} onChange={(e) => setNum('maxPbr', e.target.value)} />
        </div>
      </div>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">시가총액 범위 (억$)</label>
        <div className="flex gap-2">
          <input type="number" placeholder="최소" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
            value={filter.minCap !== undefined ? filter.minCap / 1e8 : ''} onChange={(e) => setNum('minCap', e.target.value === '' ? '' : String(Number(e.target.value) * 1e8))} />
          <input type="number" placeholder="최대" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
            value={filter.maxCap !== undefined ? filter.maxCap / 1e8 : ''} onChange={(e) => setNum('maxCap', e.target.value === '' ? '' : String(Number(e.target.value) * 1e8))} />
        </div>
      </div>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">최소 배당수익률 (%)</label>
        <input type="number" placeholder="0" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
          value={filter.minDividendYield ?? ''} onChange={(e) => setNum('minDividendYield', e.target.value)} />
      </div>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">최소 ROE (%)</label>
        <input type="number" placeholder="0" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
          value={filter.minRoe ?? ''} onChange={(e) => setNum('minRoe', e.target.value)} />
      </div>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">최대 부채비율 (%)</label>
        <input type="number" placeholder="200" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
          value={filter.maxDebtRatio ?? ''} onChange={(e) => setNum('maxDebtRatio', e.target.value)} />
      </div>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">최소 매출성장률 (%)</label>
        <input type="number" placeholder="0" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
          value={filter.minRevenueGrowth ?? ''} onChange={(e) => setNum('minRevenueGrowth', e.target.value)} />
      </div>

      <div className="mb-4">
        <label className="block text-xs text-slate-400 mb-1">최소 영업이익률 (%)</label>
        <input type="number" placeholder="0" className="w-full bg-slate-700 text-white text-sm rounded px-2 py-1.5"
          value={filter.minOperatingMargin ?? ''} onChange={(e) => setNum('minOperatingMargin', e.target.value)} />
      </div>

      <button
        onClick={handleApply}
        className="w-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold py-2 rounded"
      >
        적용
      </button>
    </div>
  );
}
