interface ApiLikeError {
  code?: string;
  message?: string;
  details?: {
    status?: number;
  };
}

export const toErrorMessage = (error: unknown, fallback: string) => {
  const apiError = error as ApiLikeError;
  const parts: string[] = [];

  if (typeof apiError?.details?.status === 'number') {
    parts.push(`HTTP ${apiError.details.status}`);
  }

  if (apiError?.code) {
    parts.push(apiError.code);
  }

  if (apiError?.message) {
    parts.push(apiError.message);
  }

  return parts.length > 0 ? parts.join(' | ') : fallback;
};

