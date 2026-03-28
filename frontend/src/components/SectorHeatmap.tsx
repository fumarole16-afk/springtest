import { Treemap, ResponsiveContainer, Tooltip } from 'recharts';
import { useNavigate } from 'react-router-dom';
import type { SectorOverview } from '../api/stockApi';

interface Props {
  sectors: SectorOverview[];
}

function colorForYield(y: number): string {
  if (y >= 3) return '#16a34a';
  if (y >= 1.5) return '#22c55e';
  if (y >= 0.5) return '#4ade80';
  return '#64748b';
}

interface ContentProps {
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  name?: string;
  sectorId?: number;
  avgDividendYield?: number;
  totalMarketCap?: number;
}

function CustomContent(props: ContentProps & { onClick: (id: number) => void }) {
  const { x = 0, y = 0, width = 0, height = 0, name, sectorId, avgDividendYield = 0, onClick } = props;
  const color = colorForYield(avgDividendYield);
  return (
    <g onClick={() => sectorId !== undefined && onClick(sectorId)} style={{ cursor: 'pointer' }}>
      <rect x={x} y={y} width={width} height={height} fill={color} stroke="#1e293b" strokeWidth={2} rx={4} />
      {width > 60 && height > 30 && (
        <text x={x + width / 2} y={y + height / 2} textAnchor="middle" dominantBaseline="central" fill="#fff" fontSize={Math.min(14, width / 8)} fontWeight={600}>
          {name}
        </text>
      )}
    </g>
  );
}

export default function SectorHeatmap({ sectors }: Props) {
  const navigate = useNavigate();

  const data = sectors.map((s) => ({
    name: s.sectorName,
    size: s.totalMarketCap || 1,
    sectorId: s.sectorId,
    avgDividendYield: s.avgDividendYield,
    totalMarketCap: s.totalMarketCap,
  }));

  return (
    <div className="bg-slate-800 rounded-lg p-4">
      <h3 className="text-sm font-semibold text-slate-300 mb-3">섹터 히트맵 (크기: 시가총액)</h3>
      <ResponsiveContainer width="100%" height={300}>
        <Treemap
          data={data}
          dataKey="size"
          content={<CustomContent onClick={(id) => navigate(`/sectors/${id}`)} />}
        >
          <Tooltip
            content={({ payload }) => {
              if (!payload || !payload[0]) return null;
              const d = payload[0].payload;
              return (
                <div className="bg-slate-900 border border-slate-700 rounded p-2 text-xs">
                  <div className="font-semibold">{d.name}</div>
                  <div className="text-slate-400">배당수익률: {d.avgDividendYield?.toFixed(2)}%</div>
                </div>
              );
            }}
          />
        </Treemap>
      </ResponsiveContainer>
    </div>
  );
}
