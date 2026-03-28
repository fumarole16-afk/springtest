import { useEffect, useState } from 'react';
import {
  getDashboardIndices,
  getDashboardMovers,
  getDashboardExtremes,
  getDashboardVolumeSpikes,
  getNews,
  getSectors,
} from '../api/stockApi';
import type { IndexQuote, MoverStock, NewsItem, SectorOverview } from '../api/stockApi';
import IndexCards from '../components/IndexCards';
import SectorHeatmap from '../components/SectorHeatmap';
import MoverTable from '../components/MoverTable';
import NewsFeed from '../components/NewsFeed';

export default function Dashboard() {
  const [indices, setIndices] = useState<IndexQuote[]>([]);
  const [gainers, setGainers] = useState<MoverStock[]>([]);
  const [losers, setLosers] = useState<MoverStock[]>([]);
  const [highs, setHighs] = useState<MoverStock[]>([]);
  const [lows, setLows] = useState<MoverStock[]>([]);
  const [volumeSpikes, setVolumeSpikes] = useState<MoverStock[]>([]);
  const [news, setNews] = useState<NewsItem[]>([]);
  const [sectors, setSectors] = useState<SectorOverview[]>([]);

  useEffect(() => {
    getDashboardIndices().then(setIndices).catch(() => setIndices([]));
    getDashboardMovers()
      .then((d) => { setGainers(d.gainers); setLosers(d.losers); })
      .catch(() => { setGainers([]); setLosers([]); });
    getDashboardExtremes()
      .then((d) => { setHighs(d.highs); setLows(d.lows); })
      .catch(() => { setHighs([]); setLows([]); });
    getDashboardVolumeSpikes().then(setVolumeSpikes).catch(() => setVolumeSpikes([]));
    getNews(0, 5).then(setNews).catch(() => setNews([]));
    getSectors().then(setSectors).catch(() => setSectors([]));
  }, []);

  return (
    <div>
      <h2 className="text-2xl font-bold mb-4">대시보드</h2>

      {/* Index Cards */}
      {indices.length > 0 && <IndexCards indices={indices} />}

      {/* Sector Heatmap */}
      {sectors.length > 0 && (
        <div className="mb-6">
          <SectorHeatmap sectors={sectors} />
        </div>
      )}

      {/* Gainers / Losers */}
      <div className="grid grid-cols-2 gap-4 mb-6">
        <MoverTable title="상승 Top 5" stocks={gainers.slice(0, 5)} />
        <MoverTable title="하락 Top 5" stocks={losers.slice(0, 5)} />
      </div>

      {/* 52-week highs / lows */}
      <div className="grid grid-cols-2 gap-4 mb-6">
        <MoverTable title="52주 신고가" stocks={highs.slice(0, 5)} />
        <MoverTable title="52주 신저가" stocks={lows.slice(0, 5)} />
      </div>

      {/* Volume spikes */}
      <div className="mb-6">
        <MoverTable title="거래량 급등" stocks={volumeSpikes.slice(0, 5)} />
      </div>

      {/* Latest news */}
      <div>
        <h3 className="text-lg font-semibold mb-3">최신 뉴스</h3>
        <NewsFeed news={news} />
      </div>
    </div>
  );
}
