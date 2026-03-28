import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from 'recharts';
import type { FinancialRatios } from '../api/stockApi';

interface Props {
  ratios: FinancialRatios;
}

const RATIO_KEYS = ['ROE', 'ROA', 'operatingMargin', 'netMargin', 'debtRatio'];
const RATIO_LABELS: Record<string, string> = {
  ROE: 'ROE',
  ROA: 'ROA',
  operatingMargin: '영업이익률',
  netMargin: '순이익률',
  debtRatio: '부채비율',
};

export default function RatioComparison({ ratios }: Props) {
  const chartData = RATIO_KEYS
    .filter((k) => ratios.stock[k] !== undefined || ratios.sectorAverage[k] !== undefined)
    .map((k) => ({
      name: RATIO_LABELS[k] ?? k,
      종목: ratios.stock[k] !== undefined ? parseFloat(ratios.stock[k].toFixed(2)) : null,
      섹터평균: ratios.sectorAverage[k] !== undefined ? parseFloat(ratios.sectorAverage[k].toFixed(2)) : null,
    }));

  if (chartData.length === 0) return null;

  return (
    <div className="bg-slate-800 rounded-lg p-4 mt-4">
      <h3 className="text-sm font-semibold text-slate-300 mb-4">섹터 대비 지표 비교</h3>
      <ResponsiveContainer width="100%" height={220}>
        <BarChart data={chartData} margin={{ top: 5, right: 10, left: 0, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
          <XAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 11 }} />
          <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} />
          <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #334155', color: '#f1f5f9' }} />
          <Legend wrapperStyle={{ color: '#94a3b8', fontSize: 12 }} />
          <Bar dataKey="종목" fill="#3b82f6" radius={[3, 3, 0, 0]} />
          <Bar dataKey="섹터평균" fill="#64748b" radius={[3, 3, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
