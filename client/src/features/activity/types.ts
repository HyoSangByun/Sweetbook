export interface ActivityMonthResponse {
  month: string;
}

export interface ActivitySummaryResponse {
  activityId: number;
  externalActivityId: string;
  activityDateTime: string;
  activityType: string;
  activityName: string;
  distanceKm: number;
  movingTimeSeconds: number;
  averageSpeed: number;
  elevationGain: number;
  calories: number;
}

export interface ActivityDetailResponse extends ActivitySummaryResponse {
  activityMonth: string;
  description: string;
  elapsedTimeSeconds: number;
}

export interface ActivityStatsResponse {
  month: string;
  totalCount: number;
  totalDistanceKm: number;
  totalMovingTimeSeconds: number;
  totalCalories: number;
  activityTypeCounts: Record<string, number>;
}

export interface ActivityImportResponse {
  importedCount: number;
  skippedCount: number;
  skippedRows: Array<{
    rowNumber: number;
    reason: string;
  }>;
}
