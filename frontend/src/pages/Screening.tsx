import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { screenStocks } from '../api/stockApi';
import type { ScreeningResult, ScreeningFilter } from '../api/stockApi';
import FilterPanel from '../components/FilterPanel';

function formatMarketCap(cap: number): string {
  if (cap >= 1e12) return `$${(cap / 1e12).toFixed(2)}T`;
  if (cap >= 1e9) return `$${(cap / 1e9).toFixed(2)}B`;
  if (cap >= 1e6) return `$${(cap / 1e6).toFixed(2)}M`;
  return `$${cap}`;
}

export default function Screening() {
  const navigate = useNavigate();
  const [results, setResults] = useState<ScreeningResult[]>([]);
  const [filter, setFilter] = useState<ScreeningFilter>({});
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  const load = useCallback((f: ScreeningFilter, p: number) => {
    setLoading(true);
    screenStocks(f, p)
      .then(({ data, meta }) => {
        setResults(data);
        if (meta) {
          const tp = meta.totalPages ?? meta.total_pages;
          if (tp !== undefined) setTotalPages(Number(tp));
        }
      })
      .catch(() => setResults([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load(filter, page);
  }, [filter, page, load]);

  function handleFilterChange(f: ScreeningFilter) {
    setFilter(f);
    setPage(0);
  }

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">종목 스크리닝</h2>
      <div className="flex gap-6 items-start">
        <FilterPanel onFilterChange={handleFilterChange} />

        <div className="flex-1 min-w-0">
          {loading ? (
            <div className="text-slate-400">로딩 중...</div>
          ) : (
            <>
              <div className="bg-slate-800 rounded-lg overflow-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-slate-400 border-b border-slate-700">
                      <th className="px-4 py-3 text-left">티커</th>
                      <th className="px-4 py-3 text-left">회사명</th>
                      <th className="px-4 py-3 text-left">거래소</th>
                      <th className="px-4 py-3 text-right">시가총액</th>
                      <th className="px-4 py-3 text-right">PER</th>
                      <th className="px-4 py-3 text-right">PBR</th>
                      <th className="px-4 py-3 text-right">ROE</th>
                      <th className="px-4 py-3 text-right">부채비율</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.length === 0 ? (
                      <tr>
                        <td colSpan={8} className="px-4 py-8 text-center text-slate-400">결과가 없습니다.</td>
                      </tr>
                    ) : results.map((r) => (
                      <tr key={r.ticker}
                        className="border-b border-slate-700 hover:bg-slate-700 cursor-pointer"
                        onClick={() => navigate(`/stocks/${r.ticker}`)}>
                        <td className="px-4 py-3 font-semibold text-blue-400">{r.ticker}</td>
                        <td className="px-4 py-3 text-slate-300">{r.companyName}</td>
                        <td className="px-4 py-3 text-slate-400">{r.exchange}</td>
                        <td className="px-4 py-3 text-right">{r.marketCap ? formatMarketCap(r.marketCap) : '-'}</td>
                        <td className="px-4 py-3 text-right">{r.per?.toFixed(2) ?? '-'}</td>
                        <td className="px-4 py-3 text-right">{r.pbr?.toFixed(2) ?? '-'}</td>
                        <td className="px-4 py-3 text-right">{r.roe != null ? `${r.roe.toFixed(1)}%` : '-'}</td>
                        <td className="px-4 py-3 text-right">{r.debtRatio != null ? `${r.debtRatio.toFixed(1)}%` : '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPages > 1 && (
                <div className="flex justify-center gap-2 mt-4">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="px-3 py-1 rounded bg-slate-700 text-slate-300 disabled:opacity-40 hover:bg-slate-600 text-sm"
                  >
                    이전
                  </button>
                  <span className="px-3 py-1 text-slate-400 text-sm">{page + 1} / {totalPages}</span>
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="px-3 py-1 rounded bg-slate-700 text-slate-300 disabled:opacity-40 hover:bg-slate-600 text-sm"
                  >
                    다음
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
