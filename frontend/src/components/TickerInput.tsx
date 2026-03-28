import { useState, useRef, useEffect } from 'react';
import { searchStocks } from '../api/stockApi';
import type { StockSearchResult } from '../api/stockApi';

interface Props {
  tickers: string[];
  onChange: (tickers: string[]) => void;
  onCompare: () => void;
}

const MAX_TICKERS = 5;

export default function TickerInput({ tickers, onChange, onCompare }: Props) {
  const [input, setInput] = useState('');
  const [suggestions, setSuggestions] = useState<StockSearchResult[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (!input.trim()) {
      setSuggestions([]);
      setShowDropdown(false);
      return;
    }
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => {
      searchStocks(input.trim())
        .then((results) => {
          setSuggestions(results.slice(0, 8));
          setShowDropdown(true);
        })
        .catch(() => setSuggestions([]));
    }, 300);
    return () => { if (timeoutRef.current) clearTimeout(timeoutRef.current); };
  }, [input]);

  function addTicker(ticker: string) {
    const t = ticker.toUpperCase();
    if (tickers.includes(t) || tickers.length >= MAX_TICKERS) return;
    onChange([...tickers, t]);
    setInput('');
    setSuggestions([]);
    setShowDropdown(false);
  }

  function removeTicker(ticker: string) {
    onChange(tickers.filter((t) => t !== ticker));
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && input.trim()) {
      addTicker(input.trim());
    }
  }

  return (
    <div className="bg-slate-800 rounded-lg p-4 mb-6">
      <div className="flex flex-wrap gap-2 mb-3">
        {tickers.map((t) => (
          <span key={t} className="flex items-center gap-1 bg-blue-600 text-white text-sm px-2 py-1 rounded">
            {t}
            <button
              onClick={() => removeTicker(t)}
              className="ml-1 text-white hover:text-red-300 font-bold leading-none"
            >
              ×
            </button>
          </span>
        ))}
      </div>
      <div className="flex gap-2 items-center">
        <div className="relative flex-1">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            onBlur={() => setTimeout(() => setShowDropdown(false), 150)}
            placeholder={tickers.length >= MAX_TICKERS ? '최대 5개' : '티커 입력 (예: AAPL)'}
            disabled={tickers.length >= MAX_TICKERS}
            className="w-full bg-slate-700 text-white rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
          />
          {showDropdown && suggestions.length > 0 && (
            <div className="absolute top-full left-0 right-0 z-10 bg-slate-900 border border-slate-700 rounded mt-1 shadow-lg max-h-48 overflow-y-auto">
              {suggestions.map((s) => (
                <button
                  key={s.ticker}
                  onMouseDown={() => addTicker(s.ticker)}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-slate-700 flex justify-between"
                >
                  <span className="text-white font-medium">{s.ticker}</span>
                  <span className="text-slate-400 truncate ml-2">{s.companyName}</span>
                </button>
              ))}
            </div>
          )}
        </div>
        <button
          onClick={onCompare}
          disabled={tickers.length < 2}
          className="px-4 py-2 bg-blue-600 text-white rounded text-sm hover:bg-blue-500 disabled:opacity-40"
        >
          비교
        </button>
      </div>
      <div className="text-xs text-slate-500 mt-2">2~5개 티커 선택 후 비교 버튼을 누르세요.</div>
    </div>
  );
}
