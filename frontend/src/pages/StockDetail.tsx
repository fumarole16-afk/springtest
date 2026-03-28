import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getStockDetail, getStockPrices, getFinancials, getFinancialRatios, getStockNews } from '../api/stockApi';
import type { StockDetail as StockDetailType, PriceData, FinancialData, FinancialRatios, NewsItem } from '../api/stockApi';
import PriceChart from '../components/PriceChart';
import FinancialTable from '../components/FinancialTable';
import RatioComparison from '../components/RatioComparison';
import NewsFeed from '../components/NewsFeed';

const PERIODS = [
  { label: '1개월', value: '1m' },
  { label: '3개월', value: '3m' },
  { label: '1년', value: '1y' },
  { label: '5년', value: '5y' },
];

export default function StockDetail() {
  const { ticker } = useParams<{ ticker: string }>();
  const [detail, setDetail] = useState<StockDetailType | null>(null);
  const [prices, setPrices] = useState<PriceData[]>([]);
  const [period, setPeriod] = useState('1m');
  const [loading, setLoading] = useState(true);
  const [financials, setFinancials] = useState<FinancialData[]>([]);
  const [ratios, setRatios] = useState<FinancialRatios | null>(null);
  const [stockNews, setStockNews] = useState<NewsItem[]>([]);

  useEffect(() => {
    if (!ticker) return;
    setLoading(true);
    Promise.all([getStockDetail(ticker), getStockPrices(ticker, period)])
      .then(([d, p]) => { setDetail(d); setPrices(p); setLoading(false); });
    getFinancials(ticker).then(setFinancials).catch(() => setFinancials([]));
    getFinancialRatios(ticker).then(setRatios).catch(() => setRatios(null));
    getStockNews(ticker).then(setStockNews).catch(() => setStockNews([]));
  }, [ticker, period]);

  if (loading) return <div className="text-slate-400">로딩 중...</div>;
  if (!detail) return <div className="text-red-400">종목을 찾을 수 없습니다.</div>;

  function formatMarketCap(cap: number): string {
    if (cap >= 1e12) return `$${(cap / 1e12).toFixed(2)}T`;
    if (cap >= 1e9) return `$${(cap / 1e9).toFixed(2)}B`;
    if (cap >= 1e6) return `$${(cap / 1e6).toFixed(2)}M`;
    return `$${cap}`;
  }

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-2xl font-bold">
          {detail.ticker}
          <span className="ml-3 text-lg text-slate-400 font-normal">{detail.companyName}</span>
        </h2>
        <div className="flex gap-4 mt-2 text-sm text-slate-400">
          <span>{detail.exchange}</span>
          {detail.sectorName && <span>{detail.sectorName}</span>}
          {detail.industryName && <span>{detail.industryName}</span>}
        </div>
      </div>
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-slate-400 text-sm">현재가</div>
          <div className="text-xl font-bold">${detail.currentPrice?.toFixed(2)}</div>
        </div>
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-slate-400 text-sm">시가총액</div>
          <div className="text-xl font-bold">{detail.marketCap ? formatMarketCap(detail.marketCap) : '-'}</div>
        </div>
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-slate-400 text-sm">PER</div>
          <div className="text-xl font-bold">{detail.trailingPE?.toFixed(2) || '-'}</div>
        </div>
      </div>
      <div className="bg-slate-800 rounded-lg p-4">
        <div className="flex gap-2 mb-4">
          {PERIODS.map((p) => (
            <button key={p.value} onClick={() => setPeriod(p.value)}
              className={`px-3 py-1 rounded text-sm ${period === p.value ? 'bg-blue-600 text-white' : 'bg-slate-700 text-slate-300 hover:bg-slate-600'}`}>
              {p.label}
            </button>
          ))}
        </div>
        <PriceChart prices={prices} />
      </div>

      <div className="mt-6">
        <h3 className="text-lg font-semibold mb-3">재무 정보</h3>
        <FinancialTable financials={financials} />
        {ratios && <RatioComparison ratios={ratios} />}
      </div>

      <div className="mt-6">
        <h3 className="text-lg font-semibold mb-3">뉴스</h3>
        <NewsFeed news={stockNews} />
      </div>
    </div>
  );
}
