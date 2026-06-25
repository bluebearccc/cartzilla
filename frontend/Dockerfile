FROM node:20-alpine

WORKDIR /app

# Sao chép package.json và package-lock.json để cài đặt dependencies
COPY package*.json ./

RUN npm install

# Sao chép toàn bộ mã nguồn frontend
COPY . .

# Mở cổng 5173
EXPOSE 5173

# Khởi chạy Vite dev server với tham số --host để lắng nghe trên 0.0.0.0
CMD ["npm", "run", "dev", "--", "--host"]
