import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
});

export interface StockSearchResult {
  ticker: string;
  companyName: string;
  exchange: string;
}

export interface StockDetail {
  ticker: string;
  companyName: string;
  exchange: string;
  marketCap: number;
  currentPrice: number;
  trailingPE: number;
  sectorName: string;
  industryName: string;
}

export interface PriceData {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  adjustedClose: number;
  volume: number;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  meta?: Record<string, unknown>;
}

export async function searchStocks(query: string): Promise<StockSearchResult[]> {
  const res = await api.get<ApiResponse<StockSearchResult[]>>('/stocks', { params: { q: query } });
  return res.data.data;
}

export async function getStockDetail(ticker: string): Promise<StockDetail> {
  const res = await api.get<ApiResponse<StockDetail>>(`/stocks/${ticker}`);
  return res.data.data;
}

export async function getStockPrices(ticker: string, period: string = '1m'): Promise<PriceData[]> {
  const res = await api.get<ApiResponse<PriceData[]>>(`/stocks/${ticker}/prices`, { params: { period } });
  return res.data.data;
}
