import { RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Legend, ResponsiveContainer, Tooltip } from 'recharts';
import type { FinancialData } from '../api/stockApi';

interface Props {
  financials: Record<string, FinancialData[]>;
  tickers: string[];
}

const COLORS = ['#3b82f6', '#f59e0b', '#10b981', '#f43f5e', '#8b5cf6'];

const METRICS = [
  { key: 'roe', label: 'ROE' },
  { key: 'roa', label: 'ROA' },
  { key: 'operatingMargin', label: '영업이익률' },
  { key: 'netMargin', label: '순이익률' },
  { key: 'debtRatio', label: '부채비율(역)' },
];

function computeMetrics(data: FinancialData[]): Record<string, number> {
  if (!data || data.length === 0) return {};
  const latest = data[data.length - 1];
  const roe = latest.totalEquity ? (latest.netIncome / latest.totalEquity) * 100 : 0;
  const roa = latest.totalAssets ? (latest.netIncome / latest.totalAssets) * 100 : 0;
  const operatingMargin = latest.revenue ? (latest.operatingIncome / latest.revenue) * 100 : 0;
  const netMargin = latest.revenue ? (latest.netIncome / latest.revenue) * 100 : 0;
  const debtRatio = latest.totalEquity ? (latest.totalLiabilities / latest.totalEquity) * 100 : 0;
  return { roe, roa, operatingMargin, netMargin, debtRatio };
}

function normalize(values: number[], invertHighIsBad = false): number[] {
  const min = Math.min(...values);
  const max = Math.max(...values);
  if (max === min) return values.map(() => 50);
  return values.map((v) => {
    const norm = ((v - min) / (max - min)) * 100;
    return invertHighIsBad ? 100 - norm : norm;
  });
}

export default function RadarComparison({ financials, tickers }: Props) {
  const rawMetrics = tickers.map((t) => computeMetrics(financials[t] || []));

  // Normalize each metric across tickers
  const normalized: Record<string, number[]> = {};
  METRICS.forEach(({ key }, _) => {
    const values = rawMetrics.map((m) => m[key] ?? 0);
    normalized[key] = normalize(values, key === 'debtRatio');
  });

  const chartData = METRICS.map(({ key, label }) => {
    const row: Record<string, string | number> = { metric: label };
    tickers.forEach((t, i) => {
      row[t] = normalized[key][i] ?? 0;
    });
    return row;
  });

  return (
    <div className="bg-slate-800 rounded-lg p-4 mb-6">
      <h3 className="text-sm font-semibold text-slate-300 mb-3">재무비율 레이더 (정규화 0-100)</h3>
      <ResponsiveContainer width="100%" height={350}>
        <RadarChart data={chartData} margin={{ top: 10, right: 30, bottom: 10, left: 30 }}>
          <PolarGrid stroke="#334155" />
          <PolarAngleAxis dataKey="metric" tick={{ fill: '#94a3b8', fontSize: 12 }} />
          <PolarRadiusAxis angle={30} domain={[0, 100]} tick={{ fill: '#64748b', fontSize: 10 }} />
          <Tooltip
            contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: 6 }}
            formatter={(value) => [Number(value).toFixed(1)]}
          />
          <Legend />
          {tickers.map((t, i) => (
            <Radar
              key={t}
              name={t}
              dataKey={t}
              stroke={COLORS[i % COLORS.length]}
              fill={COLORS[i % COLORS.length]}
              fillOpacity={0.15}
              strokeWidth={2}
            />
          ))}
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
}
