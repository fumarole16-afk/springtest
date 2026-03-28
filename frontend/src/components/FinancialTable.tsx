import { useState } from 'react';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from 'recharts';
import type { FinancialData } from '../api/stockApi';

interface Props {
  financials: FinancialData[];
}

const TABS = [
  { key: '요약', label: '요약' },
  { key: '손익계산서', label: '손익계산서' },
  { key: '대차대조표', label: '대차대조표' },
  { key: '현금흐름표', label: '현금흐름표' },
];

function fmt(n: number): string {
  if (n === undefined || n === null) return '-';
  if (Math.abs(n) >= 1e12) return `$${(n / 1e12).toFixed(2)}T`;
  if (Math.abs(n) >= 1e9) return `$${(n / 1e9).toFixed(2)}B`;
  if (Math.abs(n) >= 1e6) return `$${(n / 1e6).toFixed(2)}M`;
  return `$${n.toLocaleString()}`;
}

export default function FinancialTable({ financials }: Props) {
  const [tab, setTab] = useState('요약');

  const annual = financials.filter((f) => f.type === 'annual' || !f.type);
  const sorted = [...annual].sort((a, b) => a.period.localeCompare(b.period));

  return (
    <div className="bg-slate-800 rounded-lg p-4">
      <div className="flex gap-2 mb-4">
        {TABS.map((t) => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`px-3 py-1 rounded text-sm ${tab === t.key ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-300 hover:bg-slate-600'}`}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === '요약' && (
        <div>
          {sorted.length > 0 && (
            <div className="grid grid-cols-3 gap-4 mb-6">
              <div className="bg-slate-700 rounded p-3">
                <div className="text-xs text-slate-400 mb-1">매출 (최신)</div>
                <div className="text-lg font-bold">{fmt(sorted[sorted.length - 1]?.revenue)}</div>
              </div>
              <div className="bg-slate-700 rounded p-3">
                <div className="text-xs text-slate-400 mb-1">순이익 (최신)</div>
                <div className="text-lg font-bold">{fmt(sorted[sorted.length - 1]?.netIncome)}</div>
              </div>
              <div className="bg-slate-700 rounded p-3">
                <div className="text-xs text-slate-400 mb-1">영업이익 (최신)</div>
                <div className="text-lg font-bold">{fmt(sorted[sorted.length - 1]?.operatingIncome)}</div>
              </div>
            </div>
          )}
          {sorted.length > 0 && (
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={sorted.map((d) => ({ period: d.period, 매출: d.revenue, 순이익: d.netIncome, 영업이익: d.operatingIncome }))}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="period" tick={{ fill: '#94a3b8', fontSize: 11 }} />
                <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} tickFormatter={(v) => fmt(v)} width={70} />
                <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #334155', color: '#f1f5f9' }} formatter={(v) => fmt(v as number)} />
                <Legend wrapperStyle={{ color: '#94a3b8', fontSize: 12 }} />
                <Line type="monotone" dataKey="매출" stroke="#3b82f6" dot={false} strokeWidth={2} />
                <Line type="monotone" dataKey="순이익" stroke="#22c55e" dot={false} strokeWidth={2} />
                <Line type="monotone" dataKey="영업이익" stroke="#f59e0b" dot={false} strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          )}
          {sorted.length === 0 && <div className="text-slate-400 text-sm">재무 데이터가 없습니다.</div>}
        </div>
      )}

      {tab === '손익계산서' && (
        <YoyTable
          data={sorted}
          rows={[
            { key: 'revenue', label: '매출' },
            { key: 'operatingIncome', label: '영업이익' },
            { key: 'netIncome', label: '순이익' },
          ]}
        />
      )}

      {tab === '대차대조표' && (
        <YoyTable
          data={sorted}
          rows={[
            { key: 'totalAssets', label: '총자산' },
            { key: 'totalLiabilities', label: '총부채' },
            { key: 'totalEquity', label: '자본총계' },
          ]}
        />
      )}

      {tab === '현금흐름표' && (
        <YoyTable
          data={sorted}
          rows={[{ key: 'operatingCashFlow', label: '영업현금흐름' }]}
        />
      )}
    </div>
  );
}

function YoyTable({ data, rows }: { data: FinancialData[]; rows: { key: keyof FinancialData; label: string }[] }) {
  if (data.length === 0) return <div className="text-slate-400 text-sm">재무 데이터가 없습니다.</div>;
  return (
    <div className="overflow-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-slate-400 border-b border-slate-700">
            <th className="px-3 py-2 text-left">항목</th>
            {data.map((d) => (
              <th key={d.period} className="px-3 py-2 text-right">{d.period}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.key} className="border-b border-slate-700">
              <td className="px-3 py-2 text-slate-300">{row.label}</td>
              {data.map((d) => (
                <td key={d.period} className="px-3 py-2 text-right">{fmt(d[row.key] as number)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
