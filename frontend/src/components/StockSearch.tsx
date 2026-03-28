import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchStocks } from '../api/stockApi';
import type { StockSearchResult } from '../api/stockApi';

export default function StockSearch() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<StockSearchResult[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const navigate = useNavigate();
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (query.length < 2) { setResults([]); setIsOpen(false); return; }
    const timer = setTimeout(async () => {
      const data = await searchStocks(query);
      setResults(data);
      setIsOpen(data.length > 0);
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) setIsOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function handleSelect(ticker: string) {
    setQuery(''); setIsOpen(false); navigate(`/stocks/${ticker}`);
  }

  return (
    <div ref={wrapperRef} className="relative">
      <input type="text" value={query} onChange={(e) => setQuery(e.target.value)}
        placeholder="종목 검색..." className="w-full bg-slate-700 text-white placeholder-slate-400 px-3 py-2 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
      {isOpen && (
        <ul className="absolute z-50 mt-1 w-full bg-slate-700 rounded-lg shadow-lg max-h-60 overflow-auto">
          {results.map((r) => (
            <li key={r.ticker} onClick={() => handleSelect(r.ticker)}
              className="px-3 py-2 hover:bg-slate-600 cursor-pointer text-sm">
              <span className="font-bold text-blue-400">{r.ticker}</span>
              <span className="ml-2 text-slate-300">{r.companyName}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
