import { useEffect, useState } from 'react';
import { getNews, getFilings } from '../api/stockApi';
import type { NewsItem } from '../api/stockApi';
import NewsFeed from '../components/NewsFeed';

type Tab = 'news' | 'filings';

export default function News() {
  const [tab, setTab] = useState<Tab>('news');
  const [news, setNews] = useState<NewsItem[]>([]);
  const [filings, setFilings] = useState<NewsItem[]>([]);
  const [ticker, setTicker] = useState('');
  const [tickerInput, setTickerInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);

  useEffect(() => {
    if (tab === 'news') {
      setLoading(true);
      getNews(page, 20)
        .then(setNews)
        .catch(() => setNews([]))
        .finally(() => setLoading(false));
    }
  }, [tab, page]);

  function handleFilingsSearch() {
    const t = tickerInput.trim().toUpperCase();
    if (!t) return;
    setTicker(t);
    setLoading(true);
    getFilings(t)
      .then(setFilings)
      .catch(() => setFilings([]))
      .finally(() => setLoading(false));
  }

  return (
    <div>
      <h2 className="text-2xl font-bold mb-4">뉴스 / 공시</h2>

      <div className="flex gap-2 mb-6">
        <button
          onClick={() => setTab('news')}
          className={`px-4 py-2 rounded text-sm font-medium ${tab === 'news' ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-300 hover:bg-slate-600'}`}
        >
          뉴스
        </button>
        <button
          onClick={() => setTab('filings')}
          className={`px-4 py-2 rounded text-sm font-medium ${tab === 'filings' ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-300 hover:bg-slate-600'}`}
        >
          SEC 공시
        </button>
      </div>

      {tab === 'news' && (
        <div>
          {loading ? (
            <div className="text-slate-400">로딩 중...</div>
          ) : (
            <>
              <NewsFeed news={news} />
              <div className="flex gap-2 mt-4 justify-center">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="px-3 py-1 rounded bg-slate-700 text-slate-300 hover:bg-slate-600 disabled:opacity-40"
                >
                  이전
                </button>
                <span className="text-slate-400 text-sm self-center">{page + 1}페이지</span>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  className="px-3 py-1 rounded bg-slate-700 text-slate-300 hover:bg-slate-600"
                >
                  다음
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {tab === 'filings' && (
        <div>
          <div className="flex gap-2 mb-4">
            <input
              type="text"
              value={tickerInput}
              onChange={(e) => setTickerInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleFilingsSearch()}
              placeholder="티커 입력 (예: AAPL)"
              className="bg-slate-700 text-white rounded px-3 py-2 text-sm w-40 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              onClick={handleFilingsSearch}
              className="px-4 py-2 bg-blue-600 text-white rounded text-sm hover:bg-blue-500"
            >
              검색
            </button>
          </div>
          {loading ? (
            <div className="text-slate-400">로딩 중...</div>
          ) : ticker ? (
            <div>
              <div className="text-slate-400 text-sm mb-3">{ticker} SEC 공시</div>
              <NewsFeed news={filings} />
            </div>
          ) : (
            <div className="text-slate-400 text-sm">티커를 입력하여 공시를 검색하세요.</div>
          )}
        </div>
      )}
    </div>
  );
}
