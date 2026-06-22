import { api } from '@/lib/api';
import type { NotificationItem, NotificationPage } from '@/types/notification';

export const notificationApi = {
  list: (page = 0, size = 20) =>
    api.get<NotificationPage>('/notifications', { params: { page, size } }),
  markRead: (id: string) => api.put<NotificationItem>(`/notifications/${id}/read`),
};
