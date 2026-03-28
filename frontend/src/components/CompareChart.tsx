import { LineChart, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, CartesianGrid } from 'recharts';
import type { PriceData } from '../api/stockApi';

interface Props {
  prices: Record<string, PriceData[]>;
  tickers: string[];
}

const COLORS = ['#3b82f6', '#f59e0b', '#10b981', '#f43f5e', '#8b5cf6'];

export default function CompareChart({ prices, tickers }: Props) {
  // Build unified date list and normalize to % return from first date
  const allDates = Array.from(
    new Set(tickers.flatMap((t) => (prices[t] || []).map((p) => p.date)))
  ).sort();

  // Get first price for each ticker for normalization
  const firstPrices: Record<string, number> = {};
  tickers.forEach((t) => {
    const arr = prices[t];
    if (arr && arr.length > 0) firstPrices[t] = arr[0].adjustedClose || arr[0].close;
  });

  // Build chart data
  const priceMap: Record<string, Record<string, number>> = {};
  tickers.forEach((t) => {
    (prices[t] || []).forEach((p) => {
      if (!priceMap[p.date]) priceMap[p.date] = {};
      priceMap[p.date][t] = p.adjustedClose || p.close;
    });
  });

  const chartData = allDates.map((date) => {
    const row: Record<string, string | number> = { date };
    tickers.forEach((t) => {
      const price = priceMap[date]?.[t];
      const first = firstPrices[t];
      if (price !== undefined && first && first !== 0) {
        row[t] = parseFloat(((price / first - 1) * 100).toFixed(2));
      }
    });
    return row;
  });

  return (
    <div className="bg-slate-800 rounded-lg p-4 mb-6">
      <h3 className="text-sm font-semibold text-slate-300 mb-3">수익률 비교 (%)</h3>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
          <XAxis dataKey="date" tick={{ fill: '#94a3b8', fontSize: 11 }} tickLine={false} />
          <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} tickLine={false} tickFormatter={(v) => `${v}%`} />
          <Tooltip
            contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: 6 }}
            labelStyle={{ color: '#94a3b8' }}
            formatter={(value) => [`${Number(value).toFixed(2)}%`]}
          />
          <Legend />
          {tickers.map((t, i) => (
            <Line
              key={t}
              type="monotone"
              dataKey={t}
              stroke={COLORS[i % COLORS.length]}
              dot={false}
              strokeWidth={2}
              connectNulls
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
