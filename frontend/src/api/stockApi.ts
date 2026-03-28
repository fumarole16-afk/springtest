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

export interface ScreeningResult {
  ticker: string;
  companyName: string;
  exchange: string;
  marketCap: number;
  per: number;
  pbr: number;
  roe: number;
  debtRatio: number;
  dividendYield: number;
  revenueGrowth: number;
  operatingMargin: number;
}

export interface ScreeningFilter {
  sectorId?: number;
  minPer?: number;
  maxPer?: number;
  minPbr?: number;
  maxPbr?: number;
  minCap?: number;
  maxCap?: number;
  minDividendYield?: number;
  minRoe?: number;
  maxDebtRatio?: number;
  minRevenueGrowth?: number;
  minOperatingMargin?: number;
  exchange?: string;
}

export interface FinancialData {
  period: string;
  type: string;
  revenue: number;
  operatingIncome: number;
  netIncome: number;
  totalAssets: number;
  totalLiabilities: number;
  totalEquity: number;
  operatingCashFlow: number;
}

export interface FinancialRatios {
  stock: Record<string, number>;
  sectorAverage: Record<string, number>;
}

export interface SectorOverview {
  sectorId: number;
  sectorName: string;
  avgPer: number;
  avgPbr: number;
  totalMarketCap: number;
  avgDividendYield: number;
}

export interface SectorDetail {
  sectorId: number;
  sectorName: string;
  description: string;
  metric: SectorOverview;
  industries: IndustryDetail[];
}

export interface IndustryDetail {
  industryId: number;
  industryName: string;
  avgPer: number;
  avgPbr: number;
  totalMarketCap: number;
  stockCount: number;
  performances: Record<string, number>;
}

export async function screenStocks(filter: ScreeningFilter, page = 0, size = 20, sort = 'marketCap,desc'): Promise<{ data: ScreeningResult[]; meta: any }> {
  const params: any = { page, size, sort };
  if (filter.sectorId) params.sector = filter.sectorId;
  if (filter.minPer !== undefined) params.minPer = filter.minPer;
  if (filter.maxPer !== undefined) params.maxPer = filter.maxPer;
  if (filter.minPbr !== undefined) params.minPbr = filter.minPbr;
  if (filter.maxPbr !== undefined) params.maxPbr = filter.maxPbr;
  if (filter.minCap !== undefined) params.minCap = filter.minCap;
  if (filter.maxCap !== undefined) params.maxCap = filter.maxCap;
  if (filter.minDividendYield !== undefined) params.minDividendYield = filter.minDividendYield;
  if (filter.minRoe !== undefined) params.minRoe = filter.minRoe;
  if (filter.maxDebtRatio !== undefined) params.maxDebtRatio = filter.maxDebtRatio;
  if (filter.minRevenueGrowth !== undefined) params.minRevenueGrowth = filter.minRevenueGrowth;
  if (filter.minOperatingMargin !== undefined) params.minOperatingMargin = filter.minOperatingMargin;
  if (filter.exchange) params.exchange = filter.exchange;
  const res = await api.get<ApiResponse<ScreeningResult[]>>('/screening', { params });
  return { data: res.data.data, meta: res.data.meta };
}

export async function getFinancials(ticker: string, type = 'annual'): Promise<FinancialData[]> {
  const res = await api.get<ApiResponse<FinancialData[]>>(`/stocks/${ticker}/financials`, { params: { type } });
  return res.data.data;
}

export async function getFinancialRatios(ticker: string): Promise<FinancialRatios> {
  const res = await api.get<ApiResponse<FinancialRatios>>(`/stocks/${ticker}/financials/ratios`);
  return res.data.data;
}

export async function getSectors(): Promise<SectorOverview[]> {
  const res = await api.get<ApiResponse<SectorOverview[]>>('/sectors');
  return res.data.data;
}

export async function getSectorDetail(id: number): Promise<SectorDetail> {
  const res = await api.get<ApiResponse<SectorDetail>>(`/sectors/${id}`);
  return res.data.data;
}

export async function getIndustryDetail(id: number): Promise<IndustryDetail> {
  const res = await api.get<ApiResponse<IndustryDetail>>(`/industries/${id}`);
  return res.data.data;
}

export interface NewsItem {
  title: string;
  source: string;
  url: string;
  publishedAt: string;
  summary: string;
  imageUrl: string;
}

export interface CompareData {
  stocks: StockDetail[];
  prices: Record<string, PriceData[]>;
  financials: Record<string, FinancialData[]>;
}

export interface IndexQuote {
  symbol: string;
  name: string;
  price: number;
  change: number;
  changePercent: number;
}

export interface MoverStock {
  ticker: string;
  companyName: string;
  price: number;
  changePercent: number;
}

export async function getNews(page = 0, size = 20): Promise<NewsItem[]> {
  const res = await api.get<ApiResponse<NewsItem[]>>('/news', { params: { page, size } });
  return res.data.data;
}

export async function getStockNews(ticker: string, page = 0, size = 10): Promise<NewsItem[]> {
  const res = await api.get<ApiResponse<NewsItem[]>>(`/stocks/${ticker}/news`, { params: { page, size } });
  return res.data.data;
}

export async function getFilings(ticker: string): Promise<NewsItem[]> {
  const res = await api.get<ApiResponse<NewsItem[]>>('/news/filings', { params: { ticker } });
  return res.data.data;
}

export async function compareStocks(tickers: string[], period = '1m'): Promise<CompareData> {
  const res = await api.get<ApiResponse<CompareData>>('/compare', { params: { tickers: tickers.join(','), period } });
  return res.data.data;
}

export async function getDashboardIndices(): Promise<IndexQuote[]> {
  const res = await api.get<ApiResponse<IndexQuote[]>>('/dashboard/indices');
  return res.data.data;
}

export async function getDashboardMovers(): Promise<{gainers: MoverStock[], losers: MoverStock[]}> {
  const res = await api.get<ApiResponse<{gainers: MoverStock[], losers: MoverStock[]}>>('/dashboard/movers');
  return res.data.data;
}

export async function getDashboardExtremes(): Promise<{highs: MoverStock[], lows: MoverStock[]}> {
  const res = await api.get<ApiResponse<{highs: MoverStock[], lows: MoverStock[]}>>('/dashboard/extremes');
  return res.data.data;
}

export async function getDashboardVolumeSpikes(): Promise<MoverStock[]> {
  const res = await api.get<ApiResponse<MoverStock[]>>('/dashboard/volume-spikes');
  return res.data.data;
}
