import { useEffect, useRef } from 'react';
import { createChart, CandlestickSeries, HistogramSeries } from 'lightweight-charts';
import type { IChartApi, Time } from 'lightweight-charts';
import type { PriceData } from '../api/stockApi';

interface Props { prices: PriceData[]; }

export default function PriceChart({ prices }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);

  useEffect(() => {
    if (!containerRef.current || prices.length === 0) return;
    if (chartRef.current) chartRef.current.remove();

    const chart = createChart(containerRef.current, {
      width: containerRef.current.clientWidth, height: 400,
      layout: { background: { color: '#1e293b' }, textColor: '#94a3b8' },
      grid: { vertLines: { color: '#334155' }, horzLines: { color: '#334155' } },
      crosshair: { mode: 0 },
    });

    const candleSeries = chart.addSeries(CandlestickSeries, {
      upColor: '#22c55e', downColor: '#ef4444',
      borderUpColor: '#22c55e', borderDownColor: '#ef4444',
      wickUpColor: '#22c55e', wickDownColor: '#ef4444',
    });

    candleSeries.setData(
      prices.map((p) => ({
        time: p.date as unknown as Time,
        open: p.open, high: p.high, low: p.low, close: p.close,
      }))
    );

    const volumeSeries = chart.addSeries(HistogramSeries, {
      color: '#475569',
      priceFormat: { type: 'volume' },
      priceScaleId: 'volume',
    });
    chart.priceScale('volume').applyOptions({ scaleMargins: { top: 0.8, bottom: 0 } });
    volumeSeries.setData(
      prices.map((p) => ({
        time: p.date as unknown as Time,
        value: p.volume,
        color: p.close >= p.open ? '#22c55e40' : '#ef444440',
      }))
    );

    chart.timeScale().fitContent();
    chartRef.current = chart;

    const handleResize = () => {
      if (containerRef.current) chart.applyOptions({ width: containerRef.current.clientWidth });
    };
    window.addEventListener('resize', handleResize);
    return () => { window.removeEventListener('resize', handleResize); chart.remove(); chartRef.current = null; };
  }, [prices]);

  return <div ref={containerRef} className="w-full rounded-lg overflow-hidden" />;
}
