import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getIndustryDetail, screenStocks } from '../api/stockApi';
import type { IndustryDetail as IndustryDetailType, ScreeningResult } from '../api/stockApi';

function formatMarketCap(cap: number): string {
  if (cap >= 1e12) return `$${(cap / 1e12).toFixed(2)}T`;
  if (cap >= 1e9) return `$${(cap / 1e9).toFixed(2)}B`;
  if (cap >= 1e6) return `$${(cap / 1e6).toFixed(2)}M`;
  return `$${cap}`;
}

export default function IndustryDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<IndustryDetailType | null>(null);
  const [stocks, setStocks] = useState<ScreeningResult[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    Promise.all([
      getIndustryDetail(Number(id)),
      screenStocks({}, 0, 50),
    ])
      .then(([ind, { data }]) => {
        setDetail(ind);
        setStocks(data);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="text-slate-400">로딩 중...</div>;
  if (!detail) return <div className="text-red-400">업종 정보를 찾을 수 없습니다.</div>;

  return (
    <div>
      <button onClick={() => navigate(-1)} className="text-slate-400 hover:text-white text-sm mb-4">
        ← 뒤로
      </button>
      <h2 className="text-2xl font-bold mb-6">{detail.industryName}</h2>

      <div className="grid grid-cols-4 gap-4 mb-6">
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-xs text-slate-400 mb-1">평균 PER</div>
          <div className="text-xl font-bold">{detail.avgPer?.toFixed(2) ?? '-'}</div>
        </div>
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-xs text-slate-400 mb-1">평균 PBR</div>
          <div className="text-xl font-bold">{detail.avgPbr?.toFixed(2) ?? '-'}</div>
        </div>
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-xs text-slate-400 mb-1">시가총액</div>
          <div className="text-xl font-bold">{detail.totalMarketCap ? formatMarketCap(detail.totalMarketCap) : '-'}</div>
        </div>
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-xs text-slate-400 mb-1">종목 수</div>
          <div className="text-xl font-bold">{detail.stockCount ?? '-'}</div>
        </div>
      </div>

      <h3 className="text-lg font-semibold mb-3">종목 목록</h3>
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
            </tr>
          </thead>
          <tbody>
            {stocks.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-slate-400">종목 데이터가 없습니다.</td>
              </tr>
            ) : stocks.map((r) => (
              <tr key={r.ticker}
                className="border-b border-slate-700 hover:bg-slate-700 cursor-pointer"
                onClick={() => navigate(`/stocks/${r.ticker}`)}>
                <td className="px-4 py-3 font-semibold text-blue-400">{r.ticker}</td>
                <td className="px-4 py-3 text-slate-300">{r.companyName}</td>
                <td className="px-4 py-3 text-slate-400">{r.exchange}</td>
                <td className="px-4 py-3 text-right">{r.marketCap ? formatMarketCap(r.marketCap) : '-'}</td>
                <td className="px-4 py-3 text-right">{r.per?.toFixed(2) ?? '-'}</td>
                <td className="px-4 py-3 text-right">{r.pbr?.toFixed(2) ?? '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
