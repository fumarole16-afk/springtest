import type { IndexQuote } from '../api/stockApi';

interface Props {
  indices: IndexQuote[];
}

export default function IndexCards({ indices }: Props) {
  return (
    <div className="grid grid-cols-3 gap-4 mb-6">
      {indices.map((idx) => {
        const positive = idx.change >= 0;
        return (
          <div key={idx.symbol} className="bg-slate-800 rounded-lg p-4">
            <div className="text-slate-400 text-xs mb-1">{idx.name}</div>
            <div className="text-xl font-bold mb-1">{idx.price?.toLocaleString()}</div>
            <div className={`text-sm font-medium ${positive ? 'text-green-400' : 'text-red-400'}`}>
              {positive ? '+' : ''}{idx.change?.toFixed(2)} ({positive ? '+' : ''}{idx.changePercent?.toFixed(2)}%)
            </div>
          </div>
        );
      })}
    </div>
  );
}
