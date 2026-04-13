import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getSectorDetail } from '../api/stockApi';
import type { SectorDetail as SectorDetailType } from '../api/stockApi';

function formatMarketCap(cap: number): string {
  if (cap >= 1e12) return `$${(cap / 1e12).toFixed(2)}T`;
  if (cap >= 1e9) return `$${(cap / 1e9).toFixed(2)}B`;
  if (cap >= 1e6) return `$${(cap / 1e6).toFixed(2)}M`;
  return `$${cap}`;
}

export default function SectorDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<SectorDetailType | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getSectorDetail(Number(id))
      .then(setDetail)
      .catch(() => setDetail(null))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="text-slate-400">로딩 중...</div>;
  if (!detail) return <div className="text-red-400">섹터 정보를 찾을 수 없습니다.</div>;

  const m = detail.metric;

  return (
    <div>
      <button onClick={() => navigate('/sectors')} className="text-slate-400 hover:text-white text-sm mb-4">
        ← 섹터 목록으로
      </button>
      <h2 className="text-2xl font-bold mb-2">{detail.sectorName}</h2>
      {detail.description && <p className="text-slate-400 text-sm mb-6">{detail.description}</p>}

      {m && (
        <div className="grid grid-cols-4 gap-4 mb-6">
          <div className="bg-slate-800 rounded-lg p-4">
            <div className="text-xs text-slate-400 mb-1">평균 PER</div>
            <div className="text-xl font-bold">{m.avgPer?.toFixed(2) ?? '-'}</div>
          </div>
          <div className="bg-slate-800 rounded-lg p-4">
            <div className="text-xs text-slate-400 mb-1">평균 PBR</div>
            <div className="text-xl font-bold">{m.avgPbr?.toFixed(2) ?? '-'}</div>
          </div>
          <div className="bg-slate-800 rounded-lg p-4">
            <div className="text-xs text-slate-400 mb-1">시가총액</div>
            <div className="text-xl font-bold">{m.totalMarketCap ? formatMarketCap(m.totalMarketCap) : '-'}</div>
          </div>
          <div className="bg-slate-800 rounded-lg p-4">
            <div className="text-xs text-slate-400 mb-1">배당수익률</div>
            <div className="text-xl font-bold">{m.avgDividendYield != null ? `${m.avgDividendYield.toFixed(2)}%` : '-'}</div>
          </div>
        </div>
      )}

      <h3 className="text-lg font-semibold mb-3">업종 목록</h3>
      <div className="bg-slate-800 rounded-lg overflow-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-slate-400 border-b border-slate-700">
              <th className="px-4 py-3 text-left">업종명</th>
              <th className="px-4 py-3 text-right">평균 PER</th>
              <th className="px-4 py-3 text-right">평균 PBR</th>
              <th className="px-4 py-3 text-right">시가총액</th>
              <th className="px-4 py-3 text-right">종목 수</th>
            </tr>
          </thead>
          <tbody>
            {(!detail.industries || detail.industries.length === 0) ? (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-slate-400">업종 데이터가 없습니다.</td>
              </tr>
            ) : detail.industries.map((ind) => (
              <tr key={ind.industryId}
                className="border-b border-slate-700 hover:bg-slate-700 cursor-pointer"
                onClick={() => navigate(`/industries/${ind.industryId}`)}>
                <td className="px-4 py-3 font-semibold text-blue-400">{ind.industryName}</td>
                <td className="px-4 py-3 text-right">{ind.avgPer?.toFixed(2) ?? '-'}</td>
                <td className="px-4 py-3 text-right">{ind.avgPbr?.toFixed(2) ?? '-'}</td>
                <td className="px-4 py-3 text-right">{ind.totalMarketCap ? formatMarketCap(ind.totalMarketCap) : '-'}</td>
                <td className="px-4 py-3 text-right">{ind.stockCount ?? '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
