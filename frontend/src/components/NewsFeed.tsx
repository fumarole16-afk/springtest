import type { NewsItem } from '../api/stockApi';

interface Props {
  news: NewsItem[];
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  return d.toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' });
}

export default function NewsFeed({ news }: Props) {
  if (!news || news.length === 0) {
    return <div className="text-slate-400 text-sm py-4">뉴스가 없습니다.</div>;
  }

  return (
    <div className="flex flex-col gap-3">
      {news.map((item, i) => (
        <a
          key={i}
          href={item.url}
          target="_blank"
          rel="noopener noreferrer"
          className="flex gap-3 bg-slate-800 rounded-lg p-4 hover:bg-slate-700 transition-colors no-underline"
        >
          {item.imageUrl && (
            <img
              src={item.imageUrl}
              alt=""
              className="w-20 h-16 object-cover rounded flex-shrink-0"
              onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
            />
          )}
          <div className="flex flex-col min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs text-blue-400 font-medium">{item.source}</span>
              <span className="text-xs text-slate-500">{formatDate(item.publishedAt)}</span>
            </div>
            <div className="text-white font-medium text-sm leading-snug mb-1 line-clamp-2">
              {item.title}
            </div>
            {item.summary && (
              <div className="text-slate-400 text-xs line-clamp-2">{item.summary}</div>
            )}
          </div>
        </a>
      ))}
    </div>
  );
}
