import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { 
  ActivitySummaryResponse, 
  ActivityDetailResponse, 
  ActivityStatsResponse 
} from '../types';
import * as activityApi from '../api/activityApi';

export const useActivityStore = defineStore('activity', () => {
  const months = ref<string[]>([]);
  const currentMonth = ref<string | null>(null);
  const activities = ref<ActivitySummaryResponse[]>([]);
  const stats = ref<ActivityStatsResponse | null>(null);
  const currentActivity = ref<ActivityDetailResponse | null>(null);
  const isLoading = ref(false);

  const fetchMonths = async () => {
    const res = await activityApi.getMonths();
    months.value = res.map(m => m.month);
    if (months.value.length > 0 && !currentMonth.value) {
      currentMonth.value = months.value[0];
    }
  };

  const fetchActivities = async (month: string) => {
    isLoading.value = true;
    try {
      const res = await activityApi.getActivitiesByMonth(month);
      activities.value = res;
      currentMonth.value = month;
    } finally {
      isLoading.value = false;
    }
  };

  const fetchStats = async (month: string) => {
    const res = await activityApi.getMonthlyStats(month);
    stats.value = res;
  };

  const fetchActivityDetail = async (id: number) => {
    isLoading.value = true;
    try {
      const res = await activityApi.getActivityDetail(id);
      currentActivity.value = res;
    } finally {
      isLoading.value = false;
    }
  };

  const importActivities = async (file: File) => {
    const res = await activityApi.importCsv(file);
    await fetchMonths();
    if (currentMonth.value) {
      await fetchActivities(currentMonth.value);
      await fetchStats(currentMonth.value);
    }
    return res;
  };

  return {
    months,
    currentMonth,
    activities,
    stats,
    currentActivity,
    isLoading,
    fetchMonths,
    fetchActivities,
    fetchStats,
    fetchActivityDetail,
    importActivities,
  };
});
