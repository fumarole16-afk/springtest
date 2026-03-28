import { Link } from 'react-router-dom';
import type { MoverStock } from '../api/stockApi';

interface Props {
  title: string;
  stocks: MoverStock[];
}

export default function MoverTable({ title, stocks }: Props) {
  return (
    <div className="bg-slate-800 rounded-lg p-4">
      <h3 className="text-sm font-semibold text-slate-300 mb-3">{title}</h3>
      {!stocks || stocks.length === 0 ? (
        <div className="text-slate-400 text-sm">데이터 없음</div>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="text-slate-400 text-left">
              <th className="pb-2 pr-3">티커</th>
              <th className="pb-2 pr-3">회사명</th>
              <th className="pb-2 pr-3 text-right">현재가</th>
              <th className="pb-2 text-right">변동률</th>
            </tr>
          </thead>
          <tbody>
            {stocks.map((s) => {
              const positive = s.changePercent >= 0;
              return (
                <tr key={s.ticker} className="border-t border-slate-700">
                  <td className="py-1 pr-3">
                    <Link to={`/stocks/${s.ticker}`} className="text-blue-400 hover:underline font-medium">
                      {s.ticker}
                    </Link>
                  </td>
                  <td className="py-1 pr-3 text-slate-300 truncate max-w-[120px]">{s.companyName}</td>
                  <td className="py-1 pr-3 text-right">${s.price?.toFixed(2)}</td>
                  <td className={`py-1 text-right font-medium ${positive ? 'text-green-400' : 'text-red-400'}`}>
                    {positive ? '+' : ''}{s.changePercent?.toFixed(2)}%
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
