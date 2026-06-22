export type NotificationStatus = 'UNREAD' | 'READ';

export interface NotificationItem {
  id: string;
  orderId: string | null;
  type: string;
  title: string;
  message: string;
  status: NotificationStatus;
  priority: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationPage {
  content: NotificationItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
